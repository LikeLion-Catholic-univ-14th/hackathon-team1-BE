package com.hackthon.hackathon.dto;

import java.util.List;

public record ScheduleExtractResponse(
        String status,
        String message,
        String fileName,
        List<ExtractedSchedule> schedules
) {

    public static ScheduleExtractResponse success(
            String fileName,
            List<ExtractedSchedule> schedules
    ) {
        return new ScheduleExtractResponse(
                "SUCCESS",
                null,
                fileName,
                schedules
        );
    }

    public static ScheduleExtractResponse failed(
            String fileName,
            String message
    ) {
        return new ScheduleExtractResponse(
                "FAILED",
                message,
                fileName,
                List.of()
        );
    }

    public record ExtractedSchedule(
            String flightNumber,
            String departureAirport,
            String arrivalAirport,
            String departureTime,
            String arrivalTime,
            boolean isQuickTurn
    ) {
    }
}