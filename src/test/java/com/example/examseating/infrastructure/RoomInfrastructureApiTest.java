package com.example.examseating.infrastructure;

import com.example.examseating.dto.BuildingRequestDTO;
import com.example.examseating.dto.RoomRequestDTO;
import com.example.examseating.entity.Room;
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
public class RoomInfrastructureApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.example.examseating.repository.RoomRepository roomRepository;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    public void setup() {
        adminToken = jwtService.generateToken("admin", "ADMIN");
        studentToken = jwtService.generateToken("student", "STUDENT");
    }

    @Test
    @DisplayName("Admin can create room with building and floor associations and auto-capacity")
    public void testCreateRoomWithBuildingAndFloor() throws Exception {
        // Create Building
        BuildingRequestDTO bld = new BuildingRequestDTO();
        bld.setCode("RMB_" + System.currentTimeMillis());
        bld.setName("Academic Block");
        bld.setFloorsCount(3);

        String bldRes = mockMvc.perform(post("/api/buildings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bld)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number bldId = com.jayway.jsonpath.JsonPath.read(bldRes, "$.id");

        // Fetch Floors
        String floorRes = mockMvc.perform(get("/api/buildings/" + bldId + "/floors")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number floorId = com.jayway.jsonpath.JsonPath.read(floorRes, "$[0].id");

        // Create Room under this building and floor: 5 rows x 6 cols = 30 capacity
        RoomRequestDTO roomDto = new RoomRequestDTO();
        String roomNum = "RM_" + System.currentTimeMillis();
        roomDto.setRoomNumber(roomNum);
        roomDto.setBuildingId(bldId.longValue());
        roomDto.setFloorId(floorId.longValue());
        roomDto.setRowsCount(5);
        roomDto.setColumnsCount(6);
        roomDto.setStatus("AVAILABLE");

        mockMvc.perform(post("/api/rooms/dto")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.roomNumber", is(roomNum)))
                .andExpect(jsonPath("$.rowsCount", is(5)))
                .andExpect(jsonPath("$.columnsCount", is(6)))
                .andExpect(jsonPath("$.capacity", is(30)))
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    @DisplayName("Invalid rows (< 1) or columns (< 1) are rejected with 400 Bad Request")
    public void testInvalidRowsAndColsRejected() throws Exception {
        RoomRequestDTO invalidRoom = new RoomRequestDTO();
        invalidRoom.setRoomNumber("INV_" + System.currentTimeMillis());
        invalidRoom.setRowsCount(0); // Invalid!
        invalidRoom.setColumnsCount(4);

        mockMvc.perform(post("/api/rooms/dto")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRoom)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Room layout endpoint returns complete grid dimensions and capacity")
    public void testRoomLayoutEndpoint() throws Exception {
        Room room = roomRepository.findAll().stream().findFirst().orElseGet(() -> {
            Room r = new Room();
            r.setRoomNumber("ROOM_T_" + System.currentTimeMillis());
            r.setRowsCount(3);
            r.setColumnsCount(4);
            return roomRepository.save(r);
        });

        mockMvc.perform(get("/api/rooms/" + room.getId() + "/layout")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(room.getId().intValue())))
                .andExpect(jsonPath("$.rowsCount", is(room.getRowsCount())))
                .andExpect(jsonPath("$.columnsCount", is(room.getColumnsCount())))
                .andExpect(jsonPath("$.capacity", is(room.getRowsCount() * room.getColumnsCount())));
    }

    @Test
    @DisplayName("Room conflict check detects maintenance status")
    public void testRoomConflictCheck() throws Exception {
        // Create maintenance room
        Room room = new Room();
        room.setRoomNumber("MAINT_" + System.currentTimeMillis());
        room.setRowsCount(3);
        room.setColumnsCount(3);
        room.setStatus("MAINTENANCE");

        String res = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number rId = com.jayway.jsonpath.JsonPath.read(res, "$.id");

        mockMvc.perform(get("/api/rooms/" + rId + "/conflict")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConflict", is(true)))
                .andExpect(jsonPath("$.message", containsString("MAINTENANCE")));
    }

    @Test
    @DisplayName("Deleting room with existing seat allocations is prevented")
    public void testCannotDeleteRoomWithAllocations() throws Exception {
        // Room 1 has 12 allocations in DB
        mockMvc.perform(delete("/api/rooms/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
}
