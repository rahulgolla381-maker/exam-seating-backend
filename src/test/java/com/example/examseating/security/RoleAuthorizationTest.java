package com.example.examseating.security;

import com.example.examseating.controller.AdminTestController;
import com.example.examseating.controller.StudentMeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void test1_UnauthenticatedAdminEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test2_AdminUserAccessesAdminEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.message").value("Admin access granted"));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test3_StudentUserAccessesAdminEndpointReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void test4_UnauthenticatedStudentEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/student/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test5_StudentUserAccessesStudentEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/student/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test6_AdminUserAccessesStudentEndpointReturns403() throws Exception {
        mockMvc.perform(get("/api/student/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test7_StudentCannotGenerateAllocationReturns403() throws Exception {
        mockMvc.perform(post("/api/allocations/generate")
                .contentType("application/json")
                .content("{\"examId\": 1, \"studentIds\": [1], \"roomIds\": [1]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void test8_StudentCannotAccessAdminDepartmentsReturns403() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void test9_AdminCanAccessAdminDepartmentsReturns200() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk());
    }
}
