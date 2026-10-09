package com.example.examseating.service;

import com.example.examseating.algorithm.AllocationException;
import com.example.examseating.algorithm.AllocationResult;
import com.example.examseating.dto.AllocationResponseDTO;
import com.example.examseating.dto.GenerateAllocationRequest;
import com.example.examseating.dto.StudentAllocationResponseDTO;
import com.example.examseating.entity.Exam;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.entity.Student;
import com.example.examseating.repository.ExamRepository;
import com.example.examseating.repository.RoomRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import com.example.examseating.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SeatAllocationAppService {

    @Autowired
    private ExamRepository examRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private SeatAllocationRepository seatAllocationRepository;
    @Autowired
    private com.example.examseating.repository.AttendanceRepository attendanceRepository;
    @Autowired
    private SeatAllocationService seatAllocationService;

    @Transactional
    public List<AllocationResponseDTO> generateAllocation(GenerateAllocationRequest request) {
        if (request.getStudentIds().isEmpty() || request.getRoomIds().isEmpty()) {
            throw new IllegalArgumentException("Student list and Room list cannot be empty.");
        }
        
        if (request.getStudentIds().size() != new HashSet<>(request.getStudentIds()).size()) {
            throw new IllegalArgumentException("Duplicate students found in request.");
        }
        
        if (request.getRoomIds().size() != new HashSet<>(request.getRoomIds()).size()) {
            throw new IllegalArgumentException("Duplicate rooms found in request.");
        }

        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("Exam not found."));

        // Clear existing allocations and attendances for this exam so new seating can be generated
        deleteAllocationsByExam(exam.getId());

        List<Student> students = studentRepository.findAllById(request.getStudentIds());
        if (students.size() != request.getStudentIds().size()) {
            throw new IllegalArgumentException("One or more students not found.");
        }

        for (Student s : students) {
            if ("DETAINED".equalsIgnoreCase(s.getStatus()) || "INACTIVE".equalsIgnoreCase(s.getStatus())) {
                throw new IllegalArgumentException("Student " + s.getRollNumber() + " is " + s.getStatus() + " and not eligible for exam seating.");
            }
        }

        List<Room> rooms = roomRepository.findAllById(request.getRoomIds());
        if (rooms.size() != request.getRoomIds().size()) {
            throw new IllegalArgumentException("One or more rooms not found.");
        }

        for (Room r : rooms) {
            if (r.getStatus() != null && !"AVAILABLE".equalsIgnoreCase(r.getStatus())) {
                throw new IllegalArgumentException("Room " + r.getRoomNumber() + " is " + r.getStatus() + " and cannot be used for examination.");
            }
        }

        // Call algorithm
        List<AllocationResult> results = seatAllocationService.allocateSeating(students, rooms);

        // REQUIREMENT 9 & 10: Strict pre-persistence validation
        // Verify before saving: selected student count == allocated student count == unique allocated students
        if (results == null || results.size() != students.size()) {
            throw new AllocationException(String.format(
                "Allocation verification failed: Selected student count (%d) does not match allocated student count (%d).",
                students.size(), results == null ? 0 : results.size()
            ));
        }

        Set<Long> allocatedStudentIds = new HashSet<>();
        Set<String> allocatedSeatKeys = new HashSet<>();
        for (AllocationResult r : results) {
            if (r.getStudent() == null || r.getStudent().getId() == null) {
                throw new AllocationException("Allocation result contains invalid or null student record.");
            }
            if (!allocatedStudentIds.add(r.getStudent().getId())) {
                throw new AllocationException("Allocation verification failed: Duplicate student detected with ID " + r.getStudent().getId());
            }
            if (r.getSeatPosition() == null || r.getSeatPosition().getRoom() == null || r.getSeatPosition().getRoom().getId() == null) {
                throw new AllocationException("Allocation result contains invalid or null room/seat position.");
            }
            String seatKey = r.getSeatPosition().getRoom().getId() + "-" + r.getSeatPosition().getRow() + "-" + r.getSeatPosition().getCol();
            if (!allocatedSeatKeys.add(seatKey)) {
                throw new AllocationException("Allocation verification failed: Duplicate seat assignment detected at " + seatKey);
            }
        }

        Set<Long> expectedStudentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        if (!allocatedStudentIds.equals(expectedStudentIds)) {
            throw new AllocationException("Allocation verification failed: The set of allocated student IDs does not exactly match the set of selected student IDs.");
        }

        // Convert and Save
        List<SeatAllocation> allocations = new ArrayList<>();
        for (AllocationResult r : results) {
            SeatAllocation sa = new SeatAllocation();
            sa.setExam(exam);
            sa.setStudent(r.getStudent());
            sa.setRoom(r.getSeatPosition().getRoom());
            sa.setRowNumber(r.getSeatPosition().getRow());
            sa.setColumnNumber(r.getSeatPosition().getCol());
            sa.setSeatNumber(r.getSeatPosition().getSeatNumber());
            allocations.add(sa);
        }

        allocations = seatAllocationRepository.saveAll(allocations);

        // Post-save verification
        if (allocations.size() != students.size()) {
            throw new AllocationException(String.format(
                "Database persistence failure: Persisted %d allocations, but expected %d.",
                allocations.size(), students.size()
            ));
        }

        return allocations.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AllocationResponseDTO> getAllocationsByExam(Long examId) {
        return seatAllocationRepository.findByExamId(examId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AllocationResponseDTO> getAllocationsByExamAndRoom(Long examId, Long roomId) {
        return seatAllocationRepository.findByExamIdAndRoomId(examId, roomId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllocationsByExam(Long examId) {
        attendanceRepository.deleteByExamId(examId);
        seatAllocationRepository.deleteByExamId(examId);
    }

    /**
     * Retrieve the specific seat allocation for the authenticated student for a given examination.
     * Guarantees student privacy by returning only the student's personal allocation.
     * 
     * @param username Authenticated user handle from Security Context
     * @param examId Target Examination ID
     * @return Optional containing StudentAllocationResponseDTO if allocation exists, empty otherwise
     */
    public Optional<StudentAllocationResponseDTO> getStudentAllocation(String username, Long examId) {
        // Validate exam exists
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        // Locate student corresponding to authenticated username
        Student student = findStudentByUsername(username).orElse(null);
        if (student == null) {
            return Optional.empty();
        }

        // Find allocation for this exam and student
        Optional<SeatAllocation> allocationOpt = seatAllocationRepository.findByExamIdAndStudentId(exam.getId(), student.getId());
        return allocationOpt.map(this::toStudentDTO);
    }

    /**
     * Associate authenticated user identity with a Student entity record without database redesign.
     * Checks rollNumber first, email second, and provides development test account fallback.
     */
    public Optional<Student> findStudentByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String cleanUser = username.trim();

        // 1. Check if username matches a student roll number
        Optional<Student> student = studentRepository.findByRollNumber(cleanUser);
        if (student.isPresent()) {
            return student;
        }

        // 2. Check if username matches a student email address
        student = studentRepository.findByEmail(cleanUser);
        if (student.isPresent()) {
            return student;
        }

        // 3. Development test account fallback: 'student' maps to initial student '24EG107A01' or '23AI001'
        if ("student".equalsIgnoreCase(cleanUser)) {
            return studentRepository.findByRollNumber("24EG107A01")
                    .or(() -> studentRepository.findByRollNumber("23AI001"));
        }

        return Optional.empty();
    }

    private AllocationResponseDTO toDTO(SeatAllocation sa) {
        AllocationResponseDTO dto = new AllocationResponseDTO();
        dto.setAllocationId(sa.getId());
        dto.setExamId(sa.getExam().getId());
        dto.setRoomId(sa.getRoom().getId());
        dto.setRoomNumber(sa.getRoom().getRoomNumber());
        dto.setStudentId(sa.getStudent().getId());
        dto.setRollNumber(sa.getStudent().getRollNumber());
        dto.setStudentName(sa.getStudent().getFullName());
        dto.setDepartment(sa.getStudent().getDepartment().getCode());
        dto.setSection(sa.getStudent().getSection().getName());
        dto.setRowNumber(sa.getRowNumber());
        dto.setColumnNumber(sa.getColumnNumber());
        dto.setSeatNumber(sa.getSeatNumber());
        return dto;
    }

    private StudentAllocationResponseDTO toStudentDTO(SeatAllocation sa) {
        StudentAllocationResponseDTO dto = new StudentAllocationResponseDTO();
        dto.setExamId(sa.getExam().getId());
        dto.setSubjectName(sa.getExam().getSubjectName());
        dto.setSubjectCode(sa.getExam().getSubjectCode());
        dto.setExamDate(sa.getExam().getExamDate());
        dto.setStartTime(sa.getExam().getStartTime());
        dto.setEndTime(sa.getExam().getEndTime());
        dto.setSession(sa.getExam().getSession());
        dto.setRoomNumber(sa.getRoom().getRoomNumber());
        dto.setBuilding(sa.getRoom().getBuilding());
        dto.setFloor(sa.getRoom().getFloor());
        dto.setRowNumber(sa.getRowNumber());
        dto.setColumnNumber(sa.getColumnNumber());
        dto.setSeatNumber(sa.getSeatNumber());
        dto.setRollNumber(sa.getStudent().getRollNumber());
        dto.setStudentName(sa.getStudent().getFullName());
        dto.setDepartment(sa.getStudent().getDepartment().getCode());
        dto.setSection(sa.getStudent().getSection().getName());
        return dto;
    }
}
