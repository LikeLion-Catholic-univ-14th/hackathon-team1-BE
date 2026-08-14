package com.hackthon.hackathon.dto;

import java.util.List;

public record CalendarResponse(
        String month,
        boolean hasScheduleHistory,
        List<DayInfo> days
) {

    public record DayInfo(
            String date,
            Long scheduleId,
            String status
    ) {
    }
}