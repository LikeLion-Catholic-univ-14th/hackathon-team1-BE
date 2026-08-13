package com.hackthon.hackathon.dto;

import java.util.List;

public record MonthlyReportResponse(

        int year,
        int month,

        Summary summary,

        RouteRanking routeRanking,

        List<DailyExposure> dailyExposure,

        Trend trend,

        Analysis analysis,

        NextMonthForecast nextMonthForecast,

        Clinic clinic

) {

    public record Summary(
            int equivalentDaysInSeoul,
            int actualOutingHours
    ) {
    }

    public record RouteRanking(
            String insightMessage,
            List<Ranking> rankings
    ) {
    }

    public record Ranking(
            String route,
            int count,
            int percentage
    ) {
    }

    public record DailyExposure(
            int day,
            int outingValue,
            int indoorValue
    ) {
    }

    public record Trend(
            String comparisonText,
            List<MonthValue> months
    ) {
    }

    public record MonthValue(
            int month,
            int value
    ) {
    }

    public record Analysis(
            AnalysisItem strongestDay,
            AnalysisItem missedDays,
            AnalysisItem goodDays
    ) {
    }

    public record AnalysisItem(
            String title,
            String description,
            String tag
    ) {
    }

    public record NextMonthForecast(
            double multiplier,
            List<String> scheduledRoutes,
            String recoveryPeriod,
            String tip
    ) {
    }

    public record Clinic(
            String exposureLevel,
            int exposurePercentage,
            String description,
            String reservationUrl
    ) {
    }
}