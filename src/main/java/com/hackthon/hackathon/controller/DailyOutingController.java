package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.DailyOutingRequest;
import com.hackthon.hackathon.dto.DailyOutingResponse;
import com.hackthon.hackathon.service.DailyOutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/daily-outing")
public class DailyOutingController {

    private final DailyOutingService dailyOutingService;

    @PatchMapping
    public ResponseEntity<DailyOutingResponse> updateOuting(
            @RequestParam LocalDate date,
            @RequestBody DailyOutingRequest request
    ) {

        Long userId = 1L;

        return ResponseEntity.ok(
                dailyOutingService.updateOuting(
                        userId,
                        date,
                        request.outing()
                )
        );
    }
}