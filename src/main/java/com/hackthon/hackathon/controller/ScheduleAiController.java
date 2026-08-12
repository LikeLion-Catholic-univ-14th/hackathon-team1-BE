package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.ScheduleExtractResponse;
import com.hackthon.hackathon.service.ScheduleAiService;
import lombok.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class ScheduleAiController {
    private final ScheduleAiService scheduleAiService;

    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ScheduleExtractResponse> extractSchedule(
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(scheduleAiService.extract(image));
    }
}
