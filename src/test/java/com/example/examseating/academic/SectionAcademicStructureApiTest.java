package com.example.examseating.academic;

import com.example.examseating.dto.BatchSectionCreateDTO;
import com.example.examseating.dto.DetainedRollRequestDTO;
import com.example.examseating.dto.GenerateAllocationRequest;
import com.example.examseating.dto.SectionConfigDTO;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Exam;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.Section;
import com.example.examseating.entity.Student;
import com.example.examseating.repository.*;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SectionAcademicStructureApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private com.example.examseating.service.SectionService sectionService;

    private String adminToken;
    private Department testDept;

    @BeforeEach
    public void setup() {
        adminToken = jwtService.generateToken("admin", "ADMIN");
        testDept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("TEST_DEPT");
            d.setName("Testing Department");
            return departmentRepository.save(d);
        });
    }

    @org.junit.jupiter.api.AfterEach
    public void cleanup() {
        List<Section> testSections = sectionRepository.findAll().stream()
                .filter(s -> s.getName() != null && (
                        s.getName().startsWith("SEC_") ||
                        s.getName().startsWith("EDIT_SEC_") ||
                        s.getName().startsWith("DUP_SEC_") ||
                        s.getName().startsWith("AML-") ||
                        s.getName().startsWith("AIML-") ||
                        s.getName().startsWith("TEST_")
                ))
                .toList();
        for (Section s : testSections) {
            try {
                sectionService.deleteSectionCascade(s.getId());
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Admin can batch create custom sections with arbitrary names")
    public void testBatchCreateSections() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
        BatchSectionCreateDTO dto = new BatchSectionCreateDTO(
                testDept.getId(),
                Arrays.asList("AML-Batch-" + suffix, "AML-Core-" + suffix, "AML-Alpha-" + suffix)
        );

        mockMvc.perform(post("/api/sections/batch")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("AML-Batch-" + suffix)))
                .andExpect(jsonPath("$[1].name", is("AML-Core-" + suffix)))
                .andExpect(jsonPath("$[2].name", is("AML-Alpha-" + suffix)));
    }

    @Test
    @DisplayName("Configure roll number pattern and calculate expected count")
    public void testConfigureRollNumberPattern() throws Exception {
        Section section = new Section();
        section.setName("SEC_PAT_" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section = sectionRepository.save(section);

        SectionConfigDTO configDTO = new SectionConfigDTO("24EG999A01", "24EG999A66");

        mockMvc.perform(put("/api/sections/" + section.getId() + "/config")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startRollNumber", is("24EG999A01")))
                .andExpect(jsonPath("$.endRollNumber", is("24EG999A66")))
                .andExpect(jsonPath("$.expectedStudentCount", is(66)))
                .andExpect(jsonPath("$.eligibleStudentCount", is(66)));
    }

    @Test
    @DisplayName("Pattern validation rejects prefix mismatch")
    public void testPatternValidationPrefixMismatch() throws Exception {
        Section section = new Section();
        section.setName("SEC_MISMATCH_" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section = sectionRepository.save(section);

        SectionConfigDTO configDTO = new SectionConfigDTO("24EG999A01", "24EG999B66");

        mockMvc.perform(put("/api/sections/" + section.getId() + "/config")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("prefix mismatch")));
    }

    @Test
    @DisplayName("Pattern validation rejects start roll greater than end roll")
    public void testPatternValidationStartGreaterThanEnd() throws Exception {
        Section section = new Section();
        section.setName("SEC_INV_ORDER_" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section = sectionRepository.save(section);

        SectionConfigDTO configDTO = new SectionConfigDTO("24EG999A66", "24EG999A10");

        mockMvc.perform(put("/api/sections/" + section.getId() + "/config")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be greater than End roll number")));
    }

    @Test
    @DisplayName("Detained roll number addition, deduction from eligible count, and student sync")
    public void testDetainedManagementFlow() throws Exception {
        Section section = new Section();
        section.setName("SEC_DET_" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section = sectionRepository.save(section);

        SectionConfigDTO configDTO = new SectionConfigDTO("24EG999H01", "24EG999H60");
        mockMvc.perform(put("/api/sections/" + section.getId() + "/config")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO)))
                .andExpect(status().isOk());

        // Find or create student in DB for synchronization check
        Student student = studentRepository.findByRollNumber("24EG999H13").orElseGet(Student::new);
        student.setRollNumber("24EG999H13");
        student.setFullName("Test Detained Student");
        student.setEmail("detained_" + System.currentTimeMillis() + "@example.com");
        student.setSemester(4);
        student.setYear(2);
        student.setDepartment(testDept);
        student.setSection(section);
        student.setStatus("ACTIVE");
        student = studentRepository.save(student);

        // Add detained roll number 24EG999H13
        DetainedRollRequestDTO detDTO = new DetainedRollRequestDTO("24EG999H13");
        mockMvc.perform(post("/api/sections/" + section.getId() + "/detained")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detainedCount", is(1)))
                .andExpect(jsonPath("$.expectedStudentCount", is(60)))
                .andExpect(jsonPath("$.eligibleStudentCount", is(59)))
                .andExpect(jsonPath("$.detainedList", hasItem("24EG999H13")));

        // Verify student entity status was synchronized to DETAINED
        Student syncedStudent = studentRepository.findByRollNumber("24EG999H13").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("DETAINED", syncedStudent.getStatus());

        // Add duplicate detained roll number -> should reject
        mockMvc.perform(post("/api/sections/" + section.getId() + "/detained")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already marked as detained")));

        // Add out of range detained roll number 24EG999H99 -> should reject
        DetainedRollRequestDTO outOfRange = new DetainedRollRequestDTO("24EG999H99");
        mockMvc.perform(post("/api/sections/" + section.getId() + "/detained")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outOfRange)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("outside section range")));

        // Remove detained roll number -> restores eligible count and syncs student back to ACTIVE
        mockMvc.perform(delete("/api/sections/" + section.getId() + "/detained/24EG999H13")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detainedCount", is(0)))
                .andExpect(jsonPath("$.eligibleStudentCount", is(60)));

        Student restoredStudent = studentRepository.findByRollNumber("24EG999H13").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", restoredStudent.getStatus());

        // Clean up
        studentRepository.delete(restoredStudent);
    }

    @Test
    @DisplayName("Exam seating allocation strictly rejects DETAINED student")
    public void testExamAllocationRejectsDetainedStudent() throws Exception {
        // Create an exam
        Exam exam = new Exam();
        exam.setSubjectName("Eligibility Test Exam " + System.currentTimeMillis());
        exam.setSubjectCode("ET_" + System.currentTimeMillis());
        exam.setExamDate(LocalDate.now().plusDays(10));
        exam.setStartTime(LocalTime.of(10, 0));
        exam.setEndTime(LocalTime.of(13, 0));
        exam.setAcademicYear("2025-2026");
        exam.setSemester(4);
        exam.setSession("FN");
        exam.setStatus("SCHEDULED");
        exam = examRepository.save(exam);

        // Create room
        Room room = roomRepository.findAll().stream().findFirst().orElseThrow();

        // Create detained student
        Student student = new Student();
        student.setRollNumber("24DET" + System.currentTimeMillis());
        student.setFullName("Detained Exam Candidate");
        student.setEmail("exam_det_" + System.currentTimeMillis() + "@test.com");
        student.setSemester(4);
        student.setYear(2);
        student.setDepartment(testDept);
        Section dummySec = sectionRepository.findAll().stream().findFirst().orElseThrow();
        student.setSection(dummySec);
        student.setStatus("DETAINED");
        student = studentRepository.save(student);

        // Attempt allocation
        GenerateAllocationRequest request = new GenerateAllocationRequest();
        request.setExamId(exam.getId());
        request.setStudentIds(List.of(student.getId()));
        request.setRoomIds(List.of(room.getId()));

        mockMvc.perform(post("/api/allocations/generate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not eligible for exam seating")));

        // Clean up
        studentRepository.delete(student);
        examRepository.delete(exam);
    }

    @Test
    @DisplayName("Create section with 66 roll numbers and 6 detained gives 60 eligible")
    public void testCreateSectionWithRangeAndSixDetained() throws Exception {
        Section section = new Section();
        section.setName("AIML-A-" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section.setStartRollNumber("24EG999A01");
        section.setEndRollNumber("24EG999A66");
        section.setDetainedList(Arrays.asList(
                "24EG999A13", "24EG999A28", "24EG999A39", "24EG999A45", "24EG999A52", "24EG999A65"
        ));

        mockMvc.perform(post("/api/sections")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(section)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startRollNumber", is("24EG999A01")))
                .andExpect(jsonPath("$.endRollNumber", is("24EG999A66")))
                .andExpect(jsonPath("$.expectedStudentCount", is(66)))
                .andExpect(jsonPath("$.detainedCount", is(6)))
                .andExpect(jsonPath("$.eligibleStudentCount", is(60)))
                .andExpect(jsonPath("$.detainedList", hasSize(6)));
    }

    @Test
    @DisplayName("Duplicate section name in the same department is rejected")
    public void testDuplicateSectionNameInSameDepartmentRejected() throws Exception {
        String secName = "DUP_SEC_" + System.currentTimeMillis();
        Section section1 = new Section();
        section1.setName(secName);
        section1.setDepartment(testDept);

        mockMvc.perform(post("/api/sections")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(section1)))
                .andExpect(status().isOk());

        Section section2 = new Section();
        section2.setName(secName);
        section2.setDepartment(testDept);

        mockMvc.perform(post("/api/sections")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(section2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists in department")));
    }

    @Test
    @DisplayName("Editing section updates range and recalculates expected and eligible counts")
    public void testEditSectionRecalculatesTotals() throws Exception {
        Section section = new Section();
        section.setName("EDIT_SEC_" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section.setStartRollNumber("99EDT01A01");
        section.setEndRollNumber("99EDT01A50");
        section.setDetainedList(List.of("99EDT01A10"));
        section = sectionRepository.save(section);

        // Edit: Change end roll to 60 and add another detained roll
        section.setEndRollNumber("99EDT01A60");
        section.setDetainedList(Arrays.asList("99EDT01A10", "99EDT01A20"));

        mockMvc.perform(put("/api/sections/" + section.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(section)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedStudentCount", is(60)))
                .andExpect(jsonPath("$.detainedCount", is(2)))
                .andExpect(jsonPath("$.eligibleStudentCount", is(58)));
    }

    @Test
    @DisplayName("Safe delete rejects deletion when students are assigned to the section")
    public void testSafeDeleteRejectsWhenStudentsAssigned() throws Exception {
        Section section = new Section();
        section.setName("DEL_SEC_" + System.currentTimeMillis());
        section.setDepartment(testDept);
        section = sectionRepository.save(section);

        Student student = new Student();
        student.setRollNumber("STU_SAFE_" + System.currentTimeMillis());
        student.setFullName("Safe Delete Student");
        student.setEmail("safe_" + System.currentTimeMillis() + "@test.com");
        student.setSemester(4);
        student.setYear(2);
        student.setDepartment(testDept);
        student.setSection(section);
        student = studentRepository.save(student);

        mockMvc.perform(delete("/api/sections/" + section.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Students are currently assigned")));

        // Cleanup student first, then deletion should succeed
        studentRepository.delete(student);
        mockMvc.perform(delete("/api/sections/" + section.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}

