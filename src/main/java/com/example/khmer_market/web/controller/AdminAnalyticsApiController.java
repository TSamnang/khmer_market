package com.example.khmer_market.web.controller;

import com.example.khmer_market.service.AdminService;
import com.example.khmer_market.web.dto.AdminAnalyticsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsApiController {

    private final AdminService adminService;

    public AdminAnalyticsApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/charts")
    public ResponseEntity<AdminAnalyticsDto> getChartsData() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }
}
