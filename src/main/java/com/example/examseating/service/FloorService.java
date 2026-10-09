package com.example.examseating.service;

import com.example.examseating.dto.FloorRequestDTO;
import com.example.examseating.dto.FloorResponseDTO;
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
public class FloorService {

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private RoomRepository roomRepository;

    public List<FloorResponseDTO> getByBuildingId(Long buildingId) {
        return floorRepository.findByBuildingIdOrderByFloorNumberAsc(buildingId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Floor getById(Long id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Floor not found with ID: " + id));
    }

    @Transactional
    public FloorResponseDTO create(Long buildingId, FloorRequestDTO dto) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + buildingId));

        if (floorRepository.existsByBuildingIdAndFloorNumber(buildingId, dto.getFloorNumber())) {
            throw new IllegalArgumentException("Floor number " + dto.getFloorNumber() + " already exists in " + building.getName());
        }

        Floor floor = new Floor();
        floor.setBuilding(building);
        floor.setFloorNumber(dto.getFloorNumber());
        floor.setFloorName(dto.getFloorName().trim());

        floor = floorRepository.save(floor);
        return toDTO(floor);
    }

    @Transactional
    public FloorResponseDTO update(Long id, FloorRequestDTO dto) {
        Floor floor = getById(id);

        if (!floor.getFloorNumber().equals(dto.getFloorNumber())) {
            if (floorRepository.existsByBuildingIdAndFloorNumber(floor.getBuilding().getId(), dto.getFloorNumber())) {
                throw new IllegalArgumentException("Floor number " + dto.getFloorNumber() + " already exists in " + floor.getBuilding().getName());
            }
            floor.setFloorNumber(dto.getFloorNumber());
        }

        floor.setFloorName(dto.getFloorName().trim());
        floor = floorRepository.save(floor);
        return toDTO(floor);
    }

    @Transactional
    public void delete(Long id) {
        Floor floor = getById(id);

        long linkedRoomsCount = roomRepository.countByFloorEntityId(id);
        if (linkedRoomsCount > 0) {
            throw new IllegalStateException("Cannot delete floor '" + floor.getFloorName() + "'. " +
                    linkedRoomsCount + " rooms are currently assigned to it.");
        }

        floorRepository.delete(floor);
    }

    public FloorResponseDTO toDTO(Floor f) {
        FloorResponseDTO dto = new FloorResponseDTO();
        dto.setId(f.getId());
        dto.setBuildingId(f.getBuilding().getId());
        dto.setBuildingName(f.getBuilding().getName());
        dto.setBuildingCode(f.getBuilding().getCode());
        dto.setFloorNumber(f.getFloorNumber());
        dto.setFloorName(f.getFloorName());
        dto.setRoomsCount((int) roomRepository.countByFloorEntityId(f.getId()));
        dto.setTotalCapacity(roomRepository.sumCapacityByFloorId(f.getId()));
        return dto;
    }
}
