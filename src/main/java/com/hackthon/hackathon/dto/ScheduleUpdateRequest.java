package com.hackthon.hackathon.dto;

import java.time.LocalDateTime;

public record ScheduleUpdateRequest(
        String flightNumber,
        String departureAirport,
        String arrivalAirport,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        boolean isQuickTurn
) {
}