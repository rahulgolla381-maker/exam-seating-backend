package com.example.examseating.service;

import com.example.examseating.dto.AttendanceResponseDTO;
import com.example.examseating.dto.AttendanceSummaryDTO;
import com.example.examseating.entity.Attendance;
import com.example.examseating.entity.AttendanceStatus;
import com.example.examseating.entity.Exam;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.repository.AttendanceRepository;
import com.example.examseating.repository.ExamRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    /**
     * Retrieve all attendance records for an exam. Automatically initializes attendance
     * from existing seat allocations if no attendance records exist yet.
     */
    @Transactional
    public List<AttendanceResponseDTO> getAttendanceByExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        ensureAttendanceInitialized(exam);

        return attendanceRepository.findByExamIdOrderBySeatNumberAsc(examId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve attendance records for a specific room in an exam.
     */
    @Transactional
    public List<AttendanceResponseDTO> getAttendanceByExamAndRoom(Long examId, Long roomId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        ensureAttendanceInitialized(exam);

        return attendanceRepository.findByExamIdAndRoomIdOrderBySeatNumberAsc(examId, roomId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update individual student's attendance status.
     */
    @Transactional
    public AttendanceResponseDTO updateStudentAttendance(Long examId, Long studentId, AttendanceStatus newStatus) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        ensureAttendanceInitialized(exam);

        Attendance attendance = attendanceRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("No attendance record found for student ID " + studentId + " in exam ID " + examId));

        attendance.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        if (attendance.getMarkedAt() == null && newStatus != AttendanceStatus.PENDING) {
            attendance.setMarkedAt(now);
        }
        attendance.setUpdatedAt(now);

        attendance = attendanceRepository.save(attendance);
        return toDTO(attendance);
    }

    /**
     * Bulk update attendance records (e.g., mark all present).
     */
    @Transactional
    public List<AttendanceResponseDTO> bulkUpdateAttendance(Long examId, AttendanceStatus newStatus, Long roomId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        ensureAttendanceInitialized(exam);

        List<Attendance> list = (roomId != null)
                ? attendanceRepository.findByExamIdAndRoomIdOrderBySeatNumberAsc(examId, roomId)
                : attendanceRepository.findByExamIdOrderBySeatNumberAsc(examId);

        LocalDateTime now = LocalDateTime.now();
        for (Attendance att : list) {
            att.setStatus(newStatus);
            if (att.getMarkedAt() == null && newStatus != AttendanceStatus.PENDING) {
                att.setMarkedAt(now);
            }
            att.setUpdatedAt(now);
        }

        list = attendanceRepository.saveAll(list);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Generate summary metrics (Total, Present, Absent, Pending, %).
     */
    @Transactional
    public AttendanceSummaryDTO getAttendanceSummary(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        ensureAttendanceInitialized(exam);

        long total = attendanceRepository.countByExamId(examId);
        long present = attendanceRepository.countByExamIdAndStatus(examId, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByExamIdAndStatus(examId, AttendanceStatus.ABSENT);
        long pending = attendanceRepository.countByExamIdAndStatus(examId, AttendanceStatus.PENDING);

        return new AttendanceSummaryDTO(
                exam.getId(),
                exam.getSubjectName(),
                exam.getSubjectCode(),
                total,
                present,
                absent,
                pending
        );
    }

    /**
     * Idempotently initialize attendance records from active SeatAllocation records for the exam.
     */
    private void ensureAttendanceInitialized(Exam exam) {
        long existingCount = attendanceRepository.countByExamId(exam.getId());
        List<SeatAllocation> allocations = seatAllocationRepository.findByExamId(exam.getId());

        if (existingCount == 0 && !allocations.isEmpty()) {
            List<Attendance> records = new ArrayList<>();
            for (SeatAllocation sa : allocations) {
                Attendance att = new Attendance();
                att.setExam(exam);
                att.setStudent(sa.getStudent());
                att.setRoom(sa.getRoom());
                att.setSeatAllocation(sa);
                att.setSeatNumber(sa.getSeatNumber());
                att.setStatus(AttendanceStatus.PENDING);
                records.add(att);
            }
            attendanceRepository.saveAll(records);
        } else if (existingCount < allocations.size()) {
            // Reconcile any newly added allocations
            List<Attendance> toAdd = new ArrayList<>();
            for (SeatAllocation sa : allocations) {
                if (!attendanceRepository.existsByExamIdAndStudentId(exam.getId(), sa.getStudent().getId())) {
                    Attendance att = new Attendance();
                    att.setExam(exam);
                    att.setStudent(sa.getStudent());
                    att.setRoom(sa.getRoom());
                    att.setSeatAllocation(sa);
                    att.setSeatNumber(sa.getSeatNumber());
                    att.setStatus(AttendanceStatus.PENDING);
                    toAdd.add(att);
                }
            }
            if (!toAdd.isEmpty()) {
                attendanceRepository.saveAll(toAdd);
            }
        }
    }

    private AttendanceResponseDTO toDTO(Attendance att) {
        AttendanceResponseDTO dto = new AttendanceResponseDTO();
        dto.setId(att.getId());
        dto.setExamId(att.getExam().getId());
        dto.setSubjectName(att.getExam().getSubjectName());
        dto.setSubjectCode(att.getExam().getSubjectCode());
        dto.setStudentId(att.getStudent().getId());
        dto.setStudentName(att.getStudent().getFullName());
        dto.setRollNumber(att.getStudent().getRollNumber());
        if (att.getStudent().getDepartment() != null) {
            dto.setDepartmentCode(att.getStudent().getDepartment().getCode());
        }
        if (att.getStudent().getSection() != null) {
            dto.setSectionName(att.getStudent().getSection().getName());
        }
        dto.setRoomId(att.getRoom().getId());
        dto.setRoomNumber(att.getRoom().getRoomNumber());
        dto.setBuilding(att.getRoom().getBuilding());
        dto.setFloor(att.getRoom().getFloor());
        dto.setSeatNumber(att.getSeatNumber());
        dto.setStatus(att.getStatus());
        dto.setMarkedAt(att.getMarkedAt());
        dto.setUpdatedAt(att.getUpdatedAt());
        return dto;
    }
}
