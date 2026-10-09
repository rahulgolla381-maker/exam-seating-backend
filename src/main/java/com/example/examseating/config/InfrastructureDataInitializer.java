package com.example.examseating.config;

import com.example.examseating.entity.*;
import com.example.examseating.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class InfrastructureDataInitializer implements CommandLineRunner {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Automatic building and floor seeding disabled: Infrastructure (buildings, floors, rooms)
        // must be managed exclusively through the application UI / database without automatic recreation.

        // 2. Link existing legacy rooms to their respective building and floor entities
        List<Room> allRooms = roomRepository.findAll();
        for (Room room : allRooms) {
            if (room.getBuildingEntity() == null || room.getFloorEntity() == null) {
                String bName = room.getBuilding() != null ? room.getBuilding().trim() : "";
                Building matchingBuilding = null;
                if (bName.equalsIgnoreCase("C Block") || bName.equalsIgnoreCase("CB")) {
                    matchingBuilding = buildingRepository.findByCode("CB").orElse(null);
                } else if (bName.equalsIgnoreCase("Test Block") || bName.equalsIgnoreCase("TB")) {
                    matchingBuilding = buildingRepository.findByCode("TB").orElse(null);
                } else if (!bName.isEmpty()) {
                    matchingBuilding = buildingRepository.findByName(bName).orElse(null);
                }

                if (matchingBuilding != null) {
                    room.setBuildingEntity(matchingBuilding);
                    // Match floor (default to 1 = First Floor if floor string contains 1)
                    int floorNum = 1;
                    if (room.getFloor() != null && room.getFloor().contains("0")) {
                        floorNum = 0;
                    }
                    Floor floor = floorRepository.findByBuildingIdAndFloorNumber(matchingBuilding.getId(), floorNum)
                            .orElse(null);
                    if (floor != null) {
                        room.setFloorEntity(floor);
                    }
                    roomRepository.save(room);
                }
            }
        }

        // 3. Initialize attendance records for exams with existing allocations
        List<Exam> exams = examRepository.findAll();
        for (Exam exam : exams) {
            long existingAttendance = attendanceRepository.countByExamId(exam.getId());
            List<SeatAllocation> allocations = seatAllocationRepository.findByExamId(exam.getId());
            if (existingAttendance == 0 && !allocations.isEmpty()) {
                for (SeatAllocation sa : allocations) {
                    Attendance att = new Attendance();
                    att.setExam(exam);
                    att.setStudent(sa.getStudent());
                    att.setRoom(sa.getRoom());
                    att.setSeatAllocation(sa);
                    att.setSeatNumber(sa.getSeatNumber());
                    att.setStatus(AttendanceStatus.PENDING);
                    attendanceRepository.save(att);
                }
            }
        }
    }
}
