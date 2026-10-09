package com.example.examseating.controller;

import com.example.examseating.dto.StudentAllocationResponseDTO;
import com.example.examseating.service.SeatAllocationAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/student")
public class StudentMeController {

    @Autowired
    private SeatAllocationAppService seatAllocationAppService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> getStudentMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("username", auth != null ? auth.getName() : "student");
        response.put("role", "STUDENT");
        return ResponseEntity.ok(response);
    }

    /**
     * Secure personal seat allocation endpoint for authenticated students.
     * Determines identity exclusively from JWT security context.
     * Endpoint: GET /api/student/allocation/{examId}
     */
    @GetMapping("/allocation/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyAllocation(@PathVariable Long examId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        Optional<StudentAllocationResponseDTO> allocationOpt = seatAllocationAppService.getStudentAllocation(username, examId);

        if (allocationOpt.isEmpty()) {
            Map<String, Object> notFoundResponse = new HashMap<>();
            notFoundResponse.put("status", 404);
            notFoundResponse.put("message", "Seat Not Allocated Yet");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
        }

        return ResponseEntity.ok(allocationOpt.get());
    }
}
