package com.example.examseating.security;

import com.example.examseating.repository.SeatAllocationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentPersonalSeatApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.example.examseating.repository.ExamRepository examRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("1 & 2. Authenticated student with allocation receives 200 with their own seat data")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test1_StudentWithAllocationReturns200AndOwnData() throws Exception {
        // 'student' maps to '23AI001' which has Seat #11 in Room C-101 for Exam 1
        mockMvc.perform(get("/api/student/allocation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examId").value(1))
                .andExpect(jsonPath("$.rollNumber").value(anyOf(is("23AI001"), is("24EG107A01"))))
                .andExpect(jsonPath("$.seatNumber").exists())
                .andExpect(jsonPath("$.roomNumber").exists())
                .andExpect(jsonPath("$.department").value("AI"))
                .andExpect(jsonPath("$.section").value(anyOf(is("A"), is("Previous Batch"))));
    }

    @Test
    @DisplayName("3. Response does not leak other students' data or passwords")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test3_ResponseDoesNotContainAnotherStudentsData() throws Exception {
        mockMvc.perform(get("/api/student/allocation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value(not("23ML001")))
                .andExpect(jsonPath("$.rollNumber").value(not("23EC001")))
                .andExpect(jsonPath("$.studentName").value(not("ML Student 1")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("4. Student without allocation receives 404 'Seat Not Allocated Yet'")
    @WithMockUser(username = "23EC004", roles = {"STUDENT"})
    void test4_StudentWithoutAllocationReturnsExpectedNotAllocatedResponse() throws Exception {
        com.example.examseating.entity.Exam exam = examRepository.findById(2L).orElseGet(() -> {
            com.example.examseating.entity.Exam e = new com.example.examseating.entity.Exam();
            e.setSubjectCode("DBMS-TEST");
            e.setSubjectName("Database Management");
            e.setAcademicYear("2024-2025");
            e.setSemester(5);
            e.setExamDate(java.time.LocalDate.now());
            e.setStartTime(java.time.LocalTime.of(9, 30));
            e.setEndTime(java.time.LocalTime.of(12, 30));
            e.setSession("FN");
            return examRepository.save(e);
        });

        mockMvc.perform(get("/api/student/allocation/" + exam.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Seat Not Allocated Yet"));
    }

    @Test
    @DisplayName("5. Student requesting non-existent exam receives 400 Bad Request")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test5_StudentRequestingNonexistentExamReturnsExpectedError() throws Exception {
        mockMvc.perform(get("/api/student/allocation/99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Exam not found with ID")));
    }

    @Test
    @DisplayName("6. Unauthenticated request to student allocation returns 401")
    void test6_UnauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/student/allocation/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("7. Admin token requesting student allocation endpoint returns 403 Forbidden")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test7_AdminRequestingStudentEndpointReturns403() throws Exception {
        mockMvc.perform(get("/api/student/allocation/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("8. Tampered JWT token returns 401 Unauthorized")
    void test8_TamperedJwtReturns401() throws Exception {
        String validToken = jwtService.generateToken("student", "STUDENT");
        String tamperedToken = validToken.substring(0, validToken.length() - 8) + "tampered";

        mockMvc.perform(get("/api/student/allocation/1")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("9. Expired JWT token returns 401 Unauthorized")
    void test9_ExpiredJwtReturns401() throws Exception {
        JwtService expiredJwtService = new JwtService(jwtSecret, -1000L);
        String expiredToken = expiredJwtService.generateToken("student", "STUDENT");

        mockMvc.perform(get("/api/student/allocation/1")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("10. Admin allocation endpoint /api/allocations/{examId} remains strictly ADMIN-only")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test10_ExistingAdminAllocationEndpointRemainsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/allocations/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
