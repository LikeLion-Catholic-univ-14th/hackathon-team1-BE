package com.hackthon.hackathon.dto.today;

import java.util.List;

public record TodayResponse(
        String mode,
        UserInfo user,
        LocationInfo location,
        String currentTime,
        UvSummary uvSummary,
        SunProtection sunProtection,
        List<Solution> solutions
) {

    public record UserInfo(
            String name,
            String position
    ) {}

    public record LocationInfo(
            String city,
            String country
    ) {}

    public record UvSummary(
            String location,
            double uvIndex,
            double koreaComparison,
            int flightExposureMinutes,
            int koreaEquivalentMinutes,
            Weather weather,
            List<UvPoint> uvGraph
    ) {}

    public record Weather(
            String condition,
            int temperature
    ) {}

    public record UvPoint(
            String time,
            double uvValue
    ) {}

    public record SunProtection(
            List<String> tags,
            List<Product> products,
            String message
    ) {}

    public record Product(
            Long productId,
            String name,
            String type,
            int spf,
            boolean recommended
    ) {}

    public record Solution(
            String phase,
            String title,
            String description
    ) {}
    public static TodayResponse guest() {
        return new TodayResponse(
                "GUEST",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}