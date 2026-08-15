package com.hackthon.hackathon.dto;

import com.hackthon.hackathon.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleDailyResponse(
        Long scheduleId,
        String date,
        String route,
        String flightStatus,
        boolean isOuting,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        LocationInfo departureInfo,
        LocationInfo arrivalInfo
) {

    public record LocationInfo(
            String airportCode,
            String koreaTimeDifference,
            RiskLevel riskLevel,
            UvDetail uvDetail
    ) {
    }

    public record UvDetail(
            List<UvPoint> graph,
            String warningMessage
    ) {
    }

    public record UvPoint(
            String time,
            double uvValue
    ) {
    }
}