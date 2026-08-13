package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.MonthlyReportResponse;
import com.hackthon.hackathon.service.MonthlyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month
    ) {

        Long userId = 1L;

        return ResponseEntity.ok(
                monthlyReportService.getMonthlyReport(
                        userId,
                        year,
                        month
                )
        );
    }
}