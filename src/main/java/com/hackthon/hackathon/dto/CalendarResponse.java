package com.hackthon.hackathon.dto;

import java.util.List;

public record CalendarResponse(
        String month,
        List<DayInfo> days
) {

    public record DayInfo(
            String date,
            Long scheduleId,
            String staus
    ) {
    }
}