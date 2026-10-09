package com.example.examseating.service;

import com.example.examseating.dto.AdminDataOverviewDTO;
import com.example.examseating.entity.*;
import com.example.examseating.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDataOverviewService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private StudentRepository studentRepository;

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

    public AdminDataOverviewDTO getOverview() {
        AdminDataOverviewDTO dto = new AdminDataOverviewDTO();

        // 1. Departments
        List<Department> depts = departmentRepository.findAll();
        dto.setDepartmentsCount(depts.size());
        List<AdminDataOverviewDTO.DepartmentItem> deptItems = new ArrayList<>();
        for (Department d : depts) {
            long secCount = sectionRepository.countByDepartmentId(d.getId());
            long stuCount = studentRepository.countByDepartmentId(d.getId());
            deptItems.add(new AdminDataOverviewDTO.DepartmentItem(d.getId(), d.getCode(), d.getName(), secCount, stuCount));
        }
        dto.setDepartments(deptItems);

        // 2. Sections
        List<Section> sections = sectionRepository.findAll();
        dto.setSectionsCount(sections.size());
        List<AdminDataOverviewDTO.SectionItem> secItems = new ArrayList<>();
        for (Section s : sections) {
            long actualStudents = studentRepository.countBySectionId(s.getId());
            String rollRange = (s.getStartRollNumber() != null && s.getEndRollNumber() != null)
                    ? s.getStartRollNumber() + " – " + s.getEndRollNumber()
                    : "Not configured";
            secItems.add(new AdminDataOverviewDTO.SectionItem(
                    s.getId(),
                    s.getDepartment() != null ? s.getDepartment().getId() : null,
                    s.getDepartment() != null ? s.getDepartment().getCode() : null,
                    s.getDepartment() != null ? s.getDepartment().getName() : null,
                    s.getName(),
                    actualStudents,
                    s.getExpectedStudentCount(),
                    rollRange,
                    s.getDetainedCount(),
                    s.getEligibleStudentCount(),
                    s.isConfigured()
            ));
        }
        dto.setSections(secItems);

        // 3. Students
        List<Student> students = studentRepository.findAll();
        dto.setStudentsCount(students.size());
        List<AdminDataOverviewDTO.StudentItem> stuItems = new ArrayList<>();
        for (Student st : students) {
            stuItems.add(new AdminDataOverviewDTO.StudentItem(
                    st.getId(),
                    st.getRollNumber(),
                    st.getFullName(),
                    st.getEmail(),
                    st.getSemester(),
                    st.getYear(),
                    st.getDepartment() != null ? st.getDepartment().getCode() : null,
                    st.getSection() != null ? st.getSection().getName() : null,
                    st.getStatus()
            ));
        }
        dto.setStudents(stuItems);

        // 4. Buildings
        List<Building> buildings = buildingRepository.findAll();
        dto.setBuildingsCount(buildings.size());
        List<AdminDataOverviewDTO.BuildingItem> bldItems = new ArrayList<>();
        for (Building b : buildings) {
            bldItems.add(new AdminDataOverviewDTO.BuildingItem(b.getId(), b.getCode(), b.getName(), b.getDescription()));
        }
        dto.setBuildings(bldItems);

        // 5. Floors
        List<Floor> floors = floorRepository.findAll();
        dto.setFloorsCount(floors.size());
        List<AdminDataOverviewDTO.FloorItem> flrItems = new ArrayList<>();
        for (Floor fl : floors) {
            flrItems.add(new AdminDataOverviewDTO.FloorItem(
                    fl.getId(),
                    fl.getBuilding() != null ? fl.getBuilding().getCode() : null,
                    fl.getBuilding() != null ? fl.getBuilding().getName() : null,
                    fl.getFloorNumber(),
                    fl.getFloorName()
            ));
        }
        dto.setFloors(flrItems);

        // 6. Rooms
        List<Room> rooms = roomRepository.findAll();
        dto.setRoomsCount(rooms.size());
        List<AdminDataOverviewDTO.RoomItem> roomItems = new ArrayList<>();
        for (Room r : rooms) {
            roomItems.add(new AdminDataOverviewDTO.RoomItem(
                    r.getId(),
                    r.getRoomNumber(),
                    r.getBuilding(),
                    r.getFloor(),
                    r.getCapacity(),
                    r.getRowsCount(),
                    r.getColumnsCount()
            ));
        }
        dto.setRooms(roomItems);

        // 7. Exams
        List<Exam> exams = examRepository.findAll();
        dto.setExamsCount(exams.size());
        List<AdminDataOverviewDTO.ExamItem> examItems = new ArrayList<>();
        for (Exam ex : exams) {
            examItems.add(new AdminDataOverviewDTO.ExamItem(
                    ex.getId(),
                    ex.getSubjectCode(),
                    ex.getSubjectName(),
                    ex.getExamDate() != null ? ex.getExamDate().toString() : null,
                    ex.getStartTime() != null ? ex.getStartTime().toString() : null,
                    ex.getEndTime() != null ? ex.getEndTime().toString() : null,
                    ex.getSession(),
                    ex.getStatus()
            ));
        }
        dto.setExams(examItems);

        // 8. Seat Allocations
        List<SeatAllocation> allocations = seatAllocationRepository.findAll();
        dto.setSeatAllocationsCount(allocations.size());
        List<AdminDataOverviewDTO.AllocationItem> allocItems = new ArrayList<>();
        for (SeatAllocation sa : allocations) {
            allocItems.add(new AdminDataOverviewDTO.AllocationItem(
                    sa.getId(),
                    sa.getExam() != null ? sa.getExam().getId() : null,
                    sa.getExam() != null ? sa.getExam().getSubjectCode() : null,
                    sa.getRoom() != null ? sa.getRoom().getRoomNumber() : null,
                    sa.getSeatNumber(),
                    sa.getRowNumber(),
                    sa.getColumnNumber(),
                    sa.getStudent() != null ? sa.getStudent().getRollNumber() : null,
                    sa.getStudent() != null ? sa.getStudent().getFullName() : null
            ));
        }
        dto.setSeatAllocations(allocItems);

        // 9. Attendances
        List<Attendance> attendances = attendanceRepository.findAll();
        dto.setAttendancesCount(attendances.size());
        List<AdminDataOverviewDTO.AttendanceItem> attItems = new ArrayList<>();
        for (Attendance at : attendances) {
            attItems.add(new AdminDataOverviewDTO.AttendanceItem(
                    at.getId(),
                    at.getExam() != null ? at.getExam().getId() : null,
                    at.getExam() != null ? at.getExam().getSubjectCode() : null,
                    at.getRoom() != null ? at.getRoom().getRoomNumber() : null,
                    at.getSeatNumber(),
                    at.getStatus() != null ? at.getStatus().name() : null,
                    at.getMarkedAt() != null ? at.getMarkedAt().toString() : null,
                    at.getStudent() != null ? at.getStudent().getRollNumber() : null,
                    at.getStudent() != null ? at.getStudent().getFullName() : null
            ));
        }
        dto.setAttendances(attItems);

        return dto;
    }
}
