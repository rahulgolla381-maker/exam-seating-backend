package com.example.examseating.infrastructure;

import com.example.examseating.dto.BuildingRequestDTO;
import com.example.examseating.dto.FloorRequestDTO;
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
public class BuildingFloorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    public void setup() {
        adminToken = jwtService.generateToken("admin", "ADMIN");
        studentToken = jwtService.generateToken("student", "STUDENT");
    }

    @Test
    @DisplayName("Admin can create a building with auto-generated floors")
    public void testCreateBuildingWithFloors() throws Exception {
        BuildingRequestDTO dto = new BuildingRequestDTO();
        dto.setCode("TEST_BLD_" + System.currentTimeMillis());
        dto.setName("Innovation Hub");
        dto.setDescription("Innovation & Research Labs");
        dto.setFloorsCount(4); // Ground, 1, 2, 3

        mockMvc.perform(post("/api/buildings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Innovation Hub")))
                .andExpect(jsonPath("$.floorsCount", is(4)));
    }

    @Test
    @DisplayName("Duplicate building code is rejected with 400 Bad Request")
    public void testDuplicateBuildingCodeRejected() throws Exception {
        String uniqueCode = "DUP_" + System.currentTimeMillis();

        BuildingRequestDTO dto1 = new BuildingRequestDTO();
        dto1.setCode(uniqueCode);
        dto1.setName("Block One");

        mockMvc.perform(post("/api/buildings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        BuildingRequestDTO dto2 = new BuildingRequestDTO();
        dto2.setCode(uniqueCode);
        dto2.setName("Block Two");

        mockMvc.perform(post("/api/buildings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Buildings can have different floor counts and floors belong to building")
    public void testDifferentFloorCountsPerBuilding() throws Exception {
        // Building A with 2 floors
        BuildingRequestDTO bldA = new BuildingRequestDTO();
        bldA.setCode("BLD_A_" + System.currentTimeMillis());
        bldA.setName("Mini Complex");
        bldA.setFloorsCount(2);

        String resA = mockMvc.perform(post("/api/buildings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bldA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.floorsCount", is(2)))
                .andReturn().getResponse().getContentAsString();

        Number idA = com.jayway.jsonpath.JsonPath.read(resA, "$.id");

        // Verify floors for Building A
        mockMvc.perform(get("/api/buildings/" + idA + "/floors")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].floorName", is("Ground Floor")))
                .andExpect(jsonPath("$[1].floorName", is("First Floor")));
    }

    @Test
    @DisplayName("Admin can add custom floor to building")
    public void testAddCustomFloor() throws Exception {
        BuildingRequestDTO bld = new BuildingRequestDTO();
        bld.setCode("BLD_CUST_" + System.currentTimeMillis());
        bld.setName("Custom Block");
        bld.setFloorsCount(1); // Ground floor only

        String res = mockMvc.perform(post("/api/buildings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bld)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number bldId = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        // Add 1st Floor manually
        FloorRequestDTO floorDTO = new FloorRequestDTO();
        floorDTO.setFloorNumber(1);
        floorDTO.setFloorName("First Floor Terrace");

        mockMvc.perform(post("/api/buildings/" + bldId + "/floors")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(floorDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.floorName", is("First Floor Terrace")))
                .andExpect(jsonPath("$.buildingId", is(bldId)));
    }

    @Test
    @DisplayName("Student is forbidden from building endpoints (403)")
    public void testStudentForbiddenFromBuildings() throws Exception {
        mockMvc.perform(get("/api/buildings")
                .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to building endpoints returns 401")
    public void testUnauthenticatedForbiddenFromBuildings() throws Exception {
        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isUnauthorized());
    }
}
