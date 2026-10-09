package com.example.examseating.controller;

import com.example.examseating.dto.AttendanceResponseDTO;
import com.example.examseating.dto.AttendanceSummaryDTO;
import com.example.examseating.dto.AttendanceUpdateRequest;
import com.example.examseating.dto.BulkAttendanceRequest;
import com.example.examseating.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/{examId}")
    public List<AttendanceResponseDTO> getByExam(@PathVariable Long examId) {
        return attendanceService.getAttendanceByExam(examId);
    }

    @GetMapping("/{examId}/room/{roomId}")
    public List<AttendanceResponseDTO> getByExamAndRoom(
            @PathVariable Long examId,
            @PathVariable Long roomId) {
        return attendanceService.getAttendanceByExamAndRoom(examId, roomId);
    }

    @GetMapping("/{examId}/summary")
    public AttendanceSummaryDTO getSummary(@PathVariable Long examId) {
        return attendanceService.getAttendanceSummary(examId);
    }

    @PutMapping("/{examId}/student/{studentId}")
    public AttendanceResponseDTO updateStudentStatus(
            @PathVariable Long examId,
            @PathVariable Long studentId,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        return attendanceService.updateStudentAttendance(examId, studentId, request.getStatus());
    }

    @PostMapping("/{examId}/bulk")
    public List<AttendanceResponseDTO> bulkUpdateStatus(
            @PathVariable Long examId,
            @Valid @RequestBody BulkAttendanceRequest request) {
        return attendanceService.bulkUpdateAttendance(examId, request.getStatus(), request.getRoomId());
    }
}
