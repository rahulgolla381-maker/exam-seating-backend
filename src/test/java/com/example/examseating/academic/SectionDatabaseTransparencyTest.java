package com.example.examseating.academic;

import com.example.examseating.dto.SectionConfigDTO;
import com.example.examseating.dto.SectionDetailsDTO;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.Section;
import com.example.examseating.repository.DepartmentRepository;
import com.example.examseating.repository.SectionRepository;
import com.example.examseating.repository.StudentRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SectionDatabaseTransparencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    private String adminToken;
    private String studentToken;
    private Long testSectionId;
    private Section testSection;

    @BeforeEach
    public void setup() {
        adminToken = jwtService.generateToken("admin", "ADMIN");
        studentToken = jwtService.generateToken("student", "STUDENT");

        testSection = sectionRepository.findAll().stream()
                .filter(s -> studentRepository.countBySectionId(s.getId()) > 0)
                .findFirst().orElse(null);
        if (testSection != null) {
            testSectionId = testSection.getId();
        }
    }

    @Test
    @DisplayName("1. Section returns actual student count from database")
    public void testSection3ActualStudentCount() throws Exception {
        if (testSectionId == null) return;
        long studentCount = studentRepository.countBySectionId(testSectionId);
        mockMvc.perform(get("/api/sections/" + testSectionId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testSectionId.intValue())))
                .andExpect(jsonPath("$.actualStudentCount", is((int) studentCount)));
    }

    @Test
    @DisplayName("2. Section details API returns complete dependency breakdown")
    public void testSection3DetailsDependencies() throws Exception {
        if (testSectionId == null) return;
        long studentCount = studentRepository.countBySectionId(testSectionId);
        mockMvc.perform(get("/api/sections/" + testSectionId + "/details")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionId", is(testSectionId.intValue())))
                .andExpect(jsonPath("$.actualStudentCount", is((int) studentCount)))
                .andExpect(jsonPath("$.canDelete", is(false)))
                .andExpect(jsonPath("$.deleteBlockedReason", containsString("Cannot delete Section")));
    }

    @Test
    @DisplayName("3. Attempting to delete non-empty Section is blocked by dependency protection")
    public void testDeleteSection3Blocked() throws Exception {
        if (testSectionId == null) return;
        mockMvc.perform(delete("/api/sections/" + testSectionId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot delete this section")));
    }

    @Test
    @DisplayName("4. Admin Data Overview returns all entities and blocks non-admin")
    public void testAdminDataOverview() throws Exception {
        // Non-admin student should be 403 Forbidden
        mockMvc.perform(get("/api/admin/data-overview")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Admin should receive complete sanitized overview
        String response = mockMvc.perform(get("/api/admin/data-overview")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentsCount", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.sectionsCount", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.studentsCount", greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.departments", not(empty())))
                .andExpect(jsonPath("$.sections", not(empty())))
                .andExpect(jsonPath("$.students", not(empty())))
                .andExpect(jsonPath("$.rooms", not(empty())))
                .andExpect(jsonPath("$.exams", not(empty())))
                .andReturn().getResponse().getContentAsString();

        // Privacy & Security Check: No passwords, secrets, or tokens exposed
        assertFalse(response.contains("password"), "Response must never expose password");
        assertFalse(response.contains("secret"), "Response must never expose secret");
    }

    @Test
    @DisplayName("5. Safe delete succeeds for newly created empty section")
    public void testEmptySectionCanBeDeleted() throws Exception {
        Department dept = departmentRepository.findById(1L).orElseThrow();
        Section emptySection = new Section();
        emptySection.setName("EMPTY_SEC_" + System.currentTimeMillis());
        emptySection.setDepartment(dept);
        emptySection = sectionRepository.save(emptySection);

        // Verify details report canDelete = true
        mockMvc.perform(get("/api/sections/" + emptySection.getId() + "/details")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualStudentCount", is(0)))
                .andExpect(jsonPath("$.allocationCount", is(0)))
                .andExpect(jsonPath("$.attendanceCount", is(0)))
                .andExpect(jsonPath("$.canDelete", is(true)));

        // Perform delete
        mockMvc.perform(delete("/api/sections/" + emptySection.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify section is gone
        assertFalse(sectionRepository.existsById(emptySection.getId()), "Empty section must be removed from DB");
    }
}
