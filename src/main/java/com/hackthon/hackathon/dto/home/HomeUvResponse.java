package com.hackthon.hackathon.dto.home;

import com.hackthon.hackathon.enums.RiskLevel;

import java.util.List;

public record HomeUvResponse(
        String city,
        String country,
        String currentTime,
        String koreaTime,

        double uvIndex,
        double koreaComparison,

        int sunlightMinutes,
        int temperature,
        String weatherCondition,
        RiskLevel riskLevel,

        List<UvGraphPoint> uvGraph
) {
}