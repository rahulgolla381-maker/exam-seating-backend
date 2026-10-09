package com.example.examseating.controller;

import com.example.examseating.dto.AdminDataOverviewDTO;
import com.example.examseating.service.AdminDataOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDataOverviewController {

    @Autowired
    private AdminDataOverviewService service;

    @GetMapping("/data-overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDataOverviewDTO> getDataOverview() {
        AdminDataOverviewDTO overview = service.getOverview();
        return ResponseEntity.ok(overview);
    }
}
