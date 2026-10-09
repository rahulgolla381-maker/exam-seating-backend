package com.example.examseating.controller;

import com.example.examseating.dto.AllocationResponseDTO;
import com.example.examseating.dto.GenerateAllocationRequest;
import com.example.examseating.service.SeatAllocationAppService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
public class SeatAllocationController {

    @Autowired
    private SeatAllocationAppService appService;

    @Autowired
    private com.example.examseating.pdf.PdfGenerationService pdfService;

    @PostMapping("/generate")
    public List<AllocationResponseDTO> generate(@Valid @RequestBody GenerateAllocationRequest request) {
        return appService.generateAllocation(request);
    }

    @GetMapping("/{examId}")
    public List<AllocationResponseDTO> getByExam(@PathVariable Long examId) {
        return appService.getAllocationsByExam(examId);
    }

    @DeleteMapping("/{examId}")
    public org.springframework.http.ResponseEntity<Void> deleteByExam(@PathVariable Long examId) {
        appService.deleteAllocationsByExam(examId);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/{examId}/room/{roomId}")
    public List<AllocationResponseDTO> getByExamAndRoom(@PathVariable Long examId, @PathVariable Long roomId) {
        return appService.getAllocationsByExamAndRoom(examId, roomId);
    }

    @GetMapping("/{examId}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> getExamSeatingPdf(@PathVariable Long examId) {
        byte[] pdfBytes = pdfService.generateExamSeatingPdf(examId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("Exam_Seating_" + examId + ".pdf", java.nio.charset.StandardCharsets.UTF_8)
                .build());
        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/{examId}/room/{roomId}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> getRoomSeatingPdf(@PathVariable Long examId, @PathVariable Long roomId) {
        byte[] pdfBytes = pdfService.generateRoomSeatingPdf(examId, roomId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("Room_Seating_" + examId + "_" + roomId + ".pdf", java.nio.charset.StandardCharsets.UTF_8)
                .build());
        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/{examId}/room/{roomId}/attendance-pdf")
    public org.springframework.http.ResponseEntity<byte[]> getRoomAttendancePdf(@PathVariable Long examId, @PathVariable Long roomId) {
        byte[] pdfBytes = pdfService.generateRoomAttendancePdf(examId, roomId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("Attendance_" + examId + "_" + roomId + ".pdf", java.nio.charset.StandardCharsets.UTF_8)
                .build());
        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}

