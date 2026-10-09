package com.example.examseating.pdf;

import com.example.examseating.entity.*;
import com.example.examseating.repository.*;
import com.example.examseating.security.JwtService;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PdfGenerationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    private Long testExamId;
    private Long testRoomId;
    private String testRoomNumber;
    private String testStudentRoll;

    @BeforeEach
    void setupTestData() {
        Exam exam = examRepository.findById(1L).orElseGet(() -> {
            Exam e = new Exam();
            e.setSubjectCode("DBMS501");
            e.setSubjectName("Database Management Systems");
            e.setExamDate(LocalDate.now());
            e.setStartTime(LocalTime.of(9, 30));
            e.setEndTime(LocalTime.of(12, 30));
            e.setSemester(5);
            e.setAcademicYear("2025-2026");
            e.setSession("MORNING");
            return examRepository.save(e);
        });
        testExamId = exam.getId();

        Room room = roomRepository.findAll().stream()
                .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                .findFirst()
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setRoomNumber("C-101");
                    r.setBuilding("MAIN BLOCK");
                    r.setFloor("Floor 1");
                    r.setRowsCount(4);
                    r.setColumnsCount(4);
                    r.setStatus("AVAILABLE");
                    return roomRepository.save(r);
                });
        testRoomId = room.getId();
        testRoomNumber = room.getRoomNumber();

        List<SeatAllocation> existing = seatAllocationRepository.findByExamId(testExamId);
        if (existing.isEmpty()) {
            List<Student> students = studentRepository.findAll();
            if (!students.isEmpty()) {
                Student s = students.get(0);
                testStudentRoll = s.getRollNumber();
                SeatAllocation sa = new SeatAllocation();
                sa.setExam(exam);
                sa.setRoom(room);
                sa.setStudent(s);
                sa.setRowNumber(0);
                sa.setColumnNumber(0);
                sa.setSeatNumber(1);
                seatAllocationRepository.save(sa);
            }
        } else {
            testStudentRoll = existing.get(0).getStudent().getRollNumber();
            testRoomId = existing.get(0).getRoom().getId();
            testRoomNumber = existing.get(0).getRoom().getRoomNumber();
        }
    }

    @Test
    @DisplayName("1. Admin generates complete exam seating PDF successfully")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test1_AdminGeneratesExamSeatingPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/allocations/" + testExamId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Exam_Seating_" + testExamId + ".pdf")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array must be non-empty and reasonably sized");

        // Verify PDF signature (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"), "Generated document must start with valid PDF signature %PDF-");

        // Verify PDF content text
        PdfReader reader = new PdfReader(pdfBytes);
        String text = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();
        String normalizedText = text.replaceAll("\\s+", " ");

        assertTrue(normalizedText.contains("EXAM SEATING PLAN"), "PDF must contain document title");
        assertTrue(normalizedText.contains("PROGRAM") && normalizedText.contains("FROM") && normalizedText.contains("TO"), "PDF must contain primary seating summary table headers");
        assertTrue(normalizedText.contains("DBMS501"), "PDF must contain exam subject code");
        assertTrue(normalizedText.contains(testRoomNumber), "PDF must contain room number");
        if (testStudentRoll != null) {
            assertTrue(normalizedText.contains(testStudentRoll), "PDF must contain student roll number");
        }
    }

    @Test
    @DisplayName("2. Admin generates room seating chart PDF successfully")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test2_AdminGeneratesRoomSeatingPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/allocations/" + testExamId + "/room/" + testRoomId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Room_Seating_" + testExamId + "_" + testRoomId + ".pdf")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"));

        PdfReader reader = new PdfReader(pdfBytes);
        String text = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();

        assertTrue(text.contains("EXAMINATION HALL SEATING CHART") || text.contains("SEATING CHART"));
        assertTrue(text.contains(testRoomNumber));
    }

    @Test
    @DisplayName("3. Admin generates room attendance sheet PDF successfully")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test3_AdminGeneratesRoomAttendancePdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/allocations/" + testExamId + "/room/" + testRoomId + "/attendance-pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Attendance_" + testExamId + "_" + testRoomId + ".pdf")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"));

        PdfReader reader = new PdfReader(pdfBytes);
        String text = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();

        assertTrue(text.contains("ATTENDANCE SHEET"));
        assertTrue(text.contains("Student Signature") || text.contains("Signature"));
        assertTrue(text.contains("Invigilator Signature") || text.contains("Invigilator"));
    }

    @Test
    @DisplayName("4. Student receives 403 Forbidden for Exam Seating PDF")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test4_StudentAccessToExamPdfReturns403() throws Exception {
        mockMvc.perform(get("/api/allocations/" + testExamId + "/pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Student receives 403 Forbidden for Room Seating PDF")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test5_StudentAccessToRoomPdfReturns403() throws Exception {
        mockMvc.perform(get("/api/allocations/" + testExamId + "/room/" + testRoomId + "/pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Student receives 403 Forbidden for Attendance PDF")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test6_StudentAccessToAttendancePdfReturns403() throws Exception {
        mockMvc.perform(get("/api/allocations/" + testExamId + "/room/" + testRoomId + "/attendance-pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Unauthenticated request receives 401 Unauthorized")
    void test7_UnauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/allocations/" + testExamId + "/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Nonexistent exam returns 400 Bad Request")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test8_NonexistentExamReturns400() throws Exception {
        mockMvc.perform(get("/api/allocations/99999/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Exam not found")));
    }

    @Test
    @DisplayName("9. Exam with no allocation returns 400 Bad Request")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test9_ExamWithNoAllocationReturns400() throws Exception {
        Exam emptyExam = new Exam();
        emptyExam.setSubjectCode("EMPTY_TEST_" + System.currentTimeMillis());
        emptyExam.setSubjectName("Empty Exam");
        emptyExam.setExamDate(LocalDate.now());
        emptyExam.setStartTime(LocalTime.of(9, 30));
        emptyExam.setEndTime(LocalTime.of(12, 30));
        emptyExam.setSemester(1);
        emptyExam.setAcademicYear("2025-2026");
        emptyExam.setSession("MORNING");
        emptyExam = examRepository.save(emptyExam);

        mockMvc.perform(get("/api/allocations/" + emptyExam.getId() + "/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No seating arrangement has been generated for this exam."));
    }

    @Test
    @DisplayName("10. Nonexistent room returns 400 Bad Request")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test10_NonexistentRoomReturns400() throws Exception {
        mockMvc.perform(get("/api/allocations/" + testExamId + "/room/99999/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Room not found")));
    }

    @Test
    @DisplayName("11. Generated PDF contains exactly all allocated students and master total")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test11_PdfContainsAllAllocatedStudentsAndExactTotal() throws Exception {
        List<SeatAllocation> allocations = seatAllocationRepository.findByExamId(testExamId);
        assertFalse(allocations.isEmpty(), "Test exam must have allocations");

        MvcResult result = mockMvc.perform(get("/api/allocations/" + testExamId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdfBytes);
        int numPages = reader.getNumberOfPages();
        StringBuilder allText = new StringBuilder();
        for (int p = 1; p <= numPages; p++) {
            allText.append(PdfTextExtractor.getTextFromPage(reader, p)).append("\n");
        }
        reader.close();

        String fullPdfText = allText.toString();

        // 1. PDF total must equal the number of allocated students
        assertTrue(fullPdfText.contains("TOTAL STUDENTS ALLOCATED"), "PDF must contain total students allocated row");
        assertTrue(fullPdfText.contains(String.valueOf(allocations.size())), "PDF must display the exact count of allocated students");

        // 2. Every allocated student must appear in the PDF
        for (SeatAllocation sa : allocations) {
            String roll = sa.getStudent().getRollNumber();
            assertTrue(fullPdfText.contains(roll), "PDF must contain allocated student roll number: " + roll);
        }
    }
}
