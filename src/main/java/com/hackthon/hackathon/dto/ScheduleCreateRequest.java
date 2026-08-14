package com.hackthon.hackathon.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleCreateRequest(
        List<ScheduleItem> schedules
) {

    public record ScheduleItem(
            String flightNumber,
            String departureAirport,
            String arrivalAirport,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            boolean isQuickTurn
    ) {
    }
}