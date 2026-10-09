package com.example.examseating.attendance;

import com.example.examseating.dto.AttendanceUpdateRequest;
import com.example.examseating.dto.BulkAttendanceRequest;
import com.example.examseating.entity.AttendanceStatus;
import com.example.examseating.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AttendanceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.example.examseating.repository.SeatAllocationRepository seatAllocationRepository;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    public void setup() {
        adminToken = jwtService.generateToken("admin", "ADMIN");
        studentToken = jwtService.generateToken("student", "STUDENT");
    }

    @Test
    @DisplayName("Admin can get attendance roster for Exam 1")
    public void testGetAttendanceForExam() throws Exception {
        mockMvc.perform(get("/api/attendance/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].rollNumber").exists())
                .andExpect(jsonPath("$[0].seatNumber").exists())
                .andExpect(jsonPath("$[0].status").exists());
    }

    @Test
    @DisplayName("Admin can get room-specific attendance for Room in Exam 1")
    public void testGetAttendanceForRoom() throws Exception {
        com.example.examseating.entity.SeatAllocation sa = seatAllocationRepository.findByExamId(1L).stream()
                .findFirst().orElseThrow();
        Long roomId = sa.getRoom().getId();

        mockMvc.perform(get("/api/attendance/1/room/" + roomId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].roomId", is(roomId.intValue())));
    }

    @Test
    @DisplayName("Admin can update single student attendance to PRESENT and ABSENT")
    public void testUpdateStudentAttendance() throws Exception {
        com.example.examseating.entity.SeatAllocation sa = seatAllocationRepository.findByExamId(1L).stream()
                .findFirst().orElseThrow();
        Long studentId = sa.getStudent().getId();

        AttendanceUpdateRequest updateToPresent = new AttendanceUpdateRequest();
        updateToPresent.setStatus(AttendanceStatus.PRESENT);

        mockMvc.perform(put("/api/attendance/1/student/" + studentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateToPresent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId", is(studentId.intValue())))
                .andExpect(jsonPath("$.status", is("PRESENT")))
                .andExpect(jsonPath("$.markedAt").exists());

        // Update to ABSENT
        AttendanceUpdateRequest updateToAbsent = new AttendanceUpdateRequest();
        updateToAbsent.setStatus(AttendanceStatus.ABSENT);

        mockMvc.perform(put("/api/attendance/1/student/" + studentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateToAbsent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ABSENT")));
    }

    @Test
    @DisplayName("Admin can bulk update attendance (Mark All Present)")
    public void testBulkUpdateAttendance() throws Exception {
        int totalStudents = seatAllocationRepository.findByExamId(1L).size();

        BulkAttendanceRequest bulkRequest = new BulkAttendanceRequest();
        bulkRequest.setStatus(AttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/attendance/1/bulk")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(totalStudents)))
                .andExpect(jsonPath("$[0].status", is("PRESENT")));

        // Check summary reflects present count
        mockMvc.perform(get("/api/attendance/1/summary")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents", is(totalStudents)))
                .andExpect(jsonPath("$.presentCount", is(totalStudents)))
                .andExpect(jsonPath("$.absentCount", is(0)))
                .andExpect(jsonPath("$.pendingCount", is(0)));
    }

    @Test
    @DisplayName("Student is forbidden from viewing or modifying attendance (403)")
    public void testStudentForbiddenFromAttendance() throws Exception {
        mockMvc.perform(get("/api/attendance/1")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/attendance/1/student/1")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PRESENT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to attendance returns 401 Unauthorized")
    public void testUnauthenticatedForbiddenFromAttendance() throws Exception {
        mockMvc.perform(get("/api/attendance/1"))
                .andExpect(status().isUnauthorized());
    }
}
