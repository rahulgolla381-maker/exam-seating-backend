package com.example.examseating.service;

import com.example.examseating.dto.BuildingRequestDTO;
import com.example.examseating.dto.BuildingResponseDTO;
import com.example.examseating.entity.Building;
import com.example.examseating.entity.Floor;
import com.example.examseating.repository.BuildingRepository;
import com.example.examseating.repository.FloorRepository;
import com.example.examseating.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private RoomRepository roomRepository;

    public List<BuildingResponseDTO> getAll() {
        return buildingRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Building getById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + id));
    }

    public BuildingResponseDTO getResponseDTOById(Long id) {
        return toDTO(getById(id));
    }

    @Transactional
    public BuildingResponseDTO create(BuildingRequestDTO dto) {
        if (dto.getCode() == null || dto.getCode().trim().isBlank()) {
            throw new IllegalArgumentException("Building code cannot be empty.");
        }
        if (dto.getName() == null || dto.getName().trim().isBlank()) {
            throw new IllegalArgumentException("Building name cannot be empty.");
        }

        String cleanCode = dto.getCode().trim().toUpperCase();
        if (buildingRepository.existsByCode(cleanCode)) {
            throw new IllegalArgumentException("Building with code '" + cleanCode + "' already exists.");
        }

        Building building = new Building();
        building.setCode(cleanCode);
        building.setName(dto.getName().trim());
        building.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        building.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        building = buildingRepository.save(building);

        // Auto-generate floors if requested
        if (dto.getFloorsCount() != null && dto.getFloorsCount() > 0) {
            int count = Math.min(dto.getFloorsCount(), 50); // upper limit safety
            for (int i = 0; i < count; i++) {
                Floor floor = new Floor(building, i, Floor.getStandardFloorName(i));
                floorRepository.save(floor);
            }
        }

        return toDTO(building);
    }

    @Transactional
    public BuildingResponseDTO update(Long id, BuildingRequestDTO dto) {
        Building building = getById(id);

        String cleanCode = dto.getCode().trim().toUpperCase();
        if (buildingRepository.existsByCodeAndIdNot(cleanCode, id)) {
            throw new IllegalArgumentException("Another building with code '" + cleanCode + "' already exists.");
        }

        building.setCode(cleanCode);
        building.setName(dto.getName().trim());
        building.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        if (dto.getStatus() != null) {
            building.setStatus(dto.getStatus());
        }

        building = buildingRepository.save(building);
        return toDTO(building);
    }

    @Transactional
    public void delete(Long id) {
        Building building = getById(id);

        // Safe deletion check: prevent deletion if rooms depend on this building
        long linkedRoomsCount = roomRepository.countByBuildingEntityId(id);
        if (linkedRoomsCount > 0) {
            throw new IllegalStateException("Cannot delete building '" + building.getName() + "'. " +
                    linkedRoomsCount + " rooms are currently assigned to it.");
        }

        buildingRepository.delete(building);
    }

    public java.util.Map<String, Object> getSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalBuildings", buildingRepository.count());
        summary.put("totalFloors", floorRepository.count());
        summary.put("totalRooms", roomRepository.count());
        summary.put("totalCapacity", roomRepository.sumTotalCapacity());
        return summary;
    }

    public BuildingResponseDTO toDTO(Building b) {
        BuildingResponseDTO dto = new BuildingResponseDTO();
        dto.setId(b.getId());
        dto.setCode(b.getCode());
        dto.setName(b.getName());
        dto.setDescription(b.getDescription());
        dto.setStatus(b.getStatus());
        dto.setFloorsCount((int) floorRepository.countByBuildingId(b.getId()));
        dto.setRoomsCount((int) roomRepository.countByBuildingEntityId(b.getId()));
        dto.setTotalCapacity(roomRepository.sumCapacityByBuildingId(b.getId()));
        return dto;
    }
}
