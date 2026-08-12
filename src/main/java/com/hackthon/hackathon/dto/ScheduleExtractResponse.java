package com.hackthon.hackathon.dto;

import java.util.List;

public record ScheduleExtractResponse(
        String fileName,
        List<ExtractedSchedule> schedules
) {

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