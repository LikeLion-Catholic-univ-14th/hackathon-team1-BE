package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.*;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.service.ScheduleCalendarService;
import com.hackthon.hackathon.service.ScheduleDailyService;
import com.hackthon.hackathon.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleDailyService scheduleDailyService;
    private final ScheduleCalendarService scheduleCalendarService;

    @PostMapping
    public ResponseEntity<ScheduleCreateResponse> createSchedule(
            @RequestBody ScheduleCreateRequest request
    ) {

        Long userId = 1L;

        return ResponseEntity.ok(
                scheduleService.createSchedule(
                        userId,
                        request
                )
        );
    }
    @GetMapping("/daily")
    public ResponseEntity<ScheduleDailyResponse> getDaily(
            @RequestParam LocalDate date
    ) {

        Long userId = 1L;

        return ResponseEntity.ok(
                scheduleDailyService.getDaily(
                        userId,
                        date
                )
        );
    }
    @PatchMapping("/{scheduleId}/outing")
    public ResponseEntity<ScheduleDailyResponse> updateOuting(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleOutingRequest request
    ) {

        Schedule schedule =
                scheduleService.updateOuting(
                        scheduleId,
                        request.outing()
                );

        ScheduleDailyResponse response =
                scheduleDailyService
                        .getDailyBySchedule(
                                schedule
                        );

        return ResponseEntity.ok(
                response
        );
    }

    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ScheduleUpdateResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleUpdateRequest request
    ) {

        ScheduleUpdateResponse response =
                scheduleService.updateSchedule(
                        scheduleId,
                        request
                );

        return ResponseEntity.ok(response);
    }
    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getCalendar(
            @RequestParam String month
    ) {

        Long userId = 1L;

        return ResponseEntity.ok(
                scheduleCalendarService.getCalendar(
                        userId,
                        month
                )
        );
    }
    @PostMapping("/{scheduleId}/solution/apply")
    public ResponseEntity<Void> applySolution(
            @PathVariable Long scheduleId,
            @RequestBody SolutionApplyRequest request
    ) {

        scheduleService.applySolution(
                scheduleId,
                request
        );

        return ResponseEntity.ok().build();

    }
}