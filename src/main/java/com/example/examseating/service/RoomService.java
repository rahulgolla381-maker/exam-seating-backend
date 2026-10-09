package com.example.examseating.service;

import com.example.examseating.dto.RoomRequestDTO;
import com.example.examseating.entity.Building;
import com.example.examseating.entity.Exam;
import com.example.examseating.entity.Floor;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.repository.AttendanceRepository;
import com.example.examseating.repository.BuildingRepository;
import com.example.examseating.repository.ExamRepository;
import com.example.examseating.repository.FloorRepository;
import com.example.examseating.repository.RoomRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RoomService {

    @Autowired
    private RoomRepository repository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<Room> getAll() {
        return repository.findAll();
    }

    public Room getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Room> getByBuildingId(Long buildingId) {
        return repository.findByBuildingEntityId(buildingId);
    }

    public List<Room> getByFloorId(Long floorId) {
        return repository.findByFloorEntityId(floorId);
    }

    @Transactional
    public Room save(Room entity) {
        validateRoom(entity);
        return repository.save(entity);
    }

    @Transactional
    public Room createFromDTO(RoomRequestDTO dto) {
        Room room = new Room();
        room.setRoomNumber(dto.getRoomNumber() != null ? dto.getRoomNumber().trim() : null);
        room.setRowsCount(dto.getRowsCount() != null ? dto.getRowsCount() : 0);
        room.setColumnsCount(dto.getColumnsCount() != null ? dto.getColumnsCount() : 0);
        room.setStatus(dto.getStatus() != null ? dto.getStatus().trim() : "AVAILABLE");

        if (dto.getBuildingId() != null) {
            Building b = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + dto.getBuildingId()));
            room.setBuildingEntity(b);
            room.setBuilding(b.getName());
        } else if (dto.getBuilding() != null && !dto.getBuilding().isBlank()) {
            room.setBuilding(dto.getBuilding().trim());
        }

        if (dto.getFloorId() != null) {
            Floor f = floorRepository.findById(dto.getFloorId())
                    .orElseThrow(() -> new IllegalArgumentException("Floor not found with ID: " + dto.getFloorId()));
            room.setFloorEntity(f);
            room.setFloor(f.getFloorName());
        } else if (dto.getFloor() != null && !dto.getFloor().isBlank()) {
            room.setFloor(dto.getFloor().trim());
        }

        validateRoom(room);
        return repository.save(room);
    }

    @Transactional
    public Room updateFromDTO(Long id, RoomRequestDTO dto) {
        Room room = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));

        room.setRoomNumber(dto.getRoomNumber() != null ? dto.getRoomNumber().trim() : null);
        room.setRowsCount(dto.getRowsCount() != null ? dto.getRowsCount() : 0);
        room.setColumnsCount(dto.getColumnsCount() != null ? dto.getColumnsCount() : 0);
        if (dto.getStatus() != null) {
            room.setStatus(dto.getStatus().trim());
        }

        if (dto.getBuildingId() != null) {
            Building b = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + dto.getBuildingId()));
            room.setBuildingEntity(b);
            room.setBuilding(b.getName());
        } else if (dto.getBuilding() != null) {
            room.setBuilding(dto.getBuilding().trim());
        }

        if (dto.getFloorId() != null) {
            Floor f = floorRepository.findById(dto.getFloorId())
                    .orElseThrow(() -> new IllegalArgumentException("Floor not found with ID: " + dto.getFloorId()));
            room.setFloorEntity(f);
            room.setFloor(f.getFloorName());
        } else if (dto.getFloor() != null) {
            room.setFloor(dto.getFloor().trim());
        }

        validateRoom(room);
        return repository.save(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + id));

        // Safe deletion checks
        if (seatAllocationRepository.existsByRoomId(id)) {
            throw new IllegalStateException("Cannot delete room '" + room.getRoomNumber() +
                    "' because active seat allocations exist for this hall. Please remove or reset the allocations first.");
        }
        if (attendanceRepository.existsByRoomId(id)) {
            throw new IllegalStateException("Cannot delete room '" + room.getRoomNumber() +
                    "' because student attendance records exist for this hall.");
        }
        if (examRepository.countBySelectedRoomId(id) > 0) {
            throw new IllegalStateException("Cannot delete room '" + room.getRoomNumber() +
                    "' because it is currently assigned to one or more scheduled exams. Please unassign this room from the exam(s) first.");
        }

        repository.deleteById(id);
    }

    /**
     * Check if room has availability conflicts (MAINTENANCE/UNAVAILABLE or overlapping exam time).
     */
    public String checkRoomConflict(Long roomId, LocalDate examDate, LocalTime startTime, LocalTime endTime, Long excludeExamId) {
        Room room = repository.findById(roomId).orElse(null);
        if (room == null) return "Room not found";

        if ("MAINTENANCE".equalsIgnoreCase(room.getStatus())) {
            return "Room " + room.getRoomNumber() + " is currently under MAINTENANCE.";
        }
        if ("UNAVAILABLE".equalsIgnoreCase(room.getStatus())) {
            return "Room " + room.getRoomNumber() + " is marked UNAVAILABLE.";
        }

        if (examDate != null && startTime != null && endTime != null) {
            List<Exam> examsOnDate = examRepository.findAll().stream()
                    .filter(e -> examDate.equals(e.getExamDate()))
                    .filter(e -> excludeExamId == null || !excludeExamId.equals(e.getId()))
                    .toList();

            for (Exam other : examsOnDate) {
                // Check time overlap: (startTime < other.endTime) && (endTime > other.startTime)
                if (startTime.isBefore(other.getEndTime()) && endTime.isAfter(other.getStartTime())) {
                    // Check if other exam uses this room
                    List<SeatAllocation> otherAllocs = seatAllocationRepository.findByExamIdAndRoomId(other.getId(), roomId);
                    if (!otherAllocs.isEmpty()) {
                        return "Room " + room.getRoomNumber() + " is already occupied by exam '" +
                                other.getSubjectCode() + " - " + other.getSubjectName() + "' (" +
                                other.getStartTime() + " - " + other.getEndTime() + ").";
                    }
                }
            }
        }

        return null; // No conflict
    }

    private void validateRoom(Room room) {
        if (room.getRoomNumber() == null || room.getRoomNumber().trim().isBlank()) {
            throw new IllegalArgumentException("Room number cannot be empty.");
        }
        if (room.getRowsCount() < 1) {
            throw new IllegalArgumentException("Rows count must be at least 1.");
        }
        if (room.getColumnsCount() < 1) {
            throw new IllegalArgumentException("Columns count must be at least 1.");
        }

        String cleanNum = room.getRoomNumber().trim();
        if (room.getId() == null) {
            if (repository.existsByRoomNumber(cleanNum)) {
                throw new IllegalArgumentException("Room number '" + cleanNum + "' already exists.");
            }
        } else {
            if (repository.existsByRoomNumberAndIdNot(cleanNum, room.getId())) {
                throw new IllegalArgumentException("Another room with number '" + cleanNum + "' already exists.");
            }
        }

        if (room.getStatus() != null && !room.getStatus().isBlank()) {
            String s = room.getStatus().trim().toUpperCase();
            if (!s.equals("AVAILABLE") && !s.equals("UNAVAILABLE") && !s.equals("MAINTENANCE")) {
                room.setStatus("AVAILABLE");
            } else {
                room.setStatus(s);
            }
        } else {
            room.setStatus("AVAILABLE");
        }
    }
}
