package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.MonthlyReportResponse;
import com.hackthon.hackathon.entity.ExposureRecord;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.RiskLevel;
import com.hackthon.hackathon.repository.ExposureRecordRepository;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import com.hackthon.hackathon.util.AirportLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final UserRepository userRepository;
    private final ExposureRecordRepository exposureRecordRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(
            Long userId,
            int year,
            int month
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        YearMonth yearMonth =
                YearMonth.of(year, month);

        LocalDate startDate =
                yearMonth.atDay(1);

        LocalDate endDate =
                yearMonth.atEndOfMonth();

        List<ExposureRecord> records =
                exposureRecordRepository
                        .findByUserAndDateBetweenOrderByDateAsc(
                                user,
                                startDate,
                                endDate
                        );

        MonthlyReportResponse.Summary summary =
                createSummary(records);

        MonthlyReportResponse.RouteRanking routeRanking =
                createRouteRanking(records);

        List<MonthlyReportResponse.DailyExposure> dailyExposure =
                createDailyExposure(
                        yearMonth,
                        records
                );

        MonthlyReportResponse.Trend trend =
                createTrend(
                        user,
                        yearMonth
                );

        MonthlyReportResponse.Analysis analysis =
                createAnalysis(records);

        MonthlyReportResponse.NextMonthForecast nextMonthForecast =
                createNextMonthForecast(
                        user,
                        yearMonth,
                        records
                );

        MonthlyReportResponse.Clinic clinic =
                createClinic(
                        user,
                        yearMonth,
                        nextMonthForecast
                );

        return new MonthlyReportResponse(
                year,
                month,
                summary,
                routeRanking,
                dailyExposure,
                trend,
                analysis,
                nextMonthForecast,
                clinic
        );
    }


    // =========================
    // SUMMARY
    // =========================

    private MonthlyReportResponse.Summary createSummary(
            List<ExposureRecord> records
    ) {

        double equivalentDays =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .map(ExposureRecord::getKoreaComparison)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum();

        int outingMinutes =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .map(ExposureRecord::getSunlightMinutes)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();

        return new MonthlyReportResponse.Summary(
                (int) Math.round(equivalentDays),
                (int) Math.round(
                        outingMinutes / 60.0
                )
        );
    }


    // =========================
    // ROUTE RANKING
    // =========================

    private MonthlyReportResponse.RouteRanking createRouteRanking(
            List<ExposureRecord> records
    ) {

        Map<String, List<ExposureRecord>> grouped =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .collect(
                                Collectors.groupingBy(
                                        ExposureRecord::getAirportCode
                                )
                        );

        double totalScore =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .map(ExposureRecord::getEstimatedExposureScore)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum();

        List<MonthlyReportResponse.Ranking> rankings =
                new ArrayList<>();

        for (Map.Entry<String, List<ExposureRecord>> entry
                : grouped.entrySet()) {

            String airportCode =
                    entry.getKey();

            List<ExposureRecord> routeRecords =
                    entry.getValue();

            double routeScore =
                    routeRecords.stream()
                            .map(ExposureRecord::getEstimatedExposureScore)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

            int percentage =
                    totalScore <= 0
                            ? 0
                            : (int) Math.round(
                            routeScore
                            / totalScore
                            * 100
                    );

            long count =
                    routeRecords.stream()
                            .map(record ->
                                    record.getSchedule().getId()
                            )
                            .distinct()
                            .count();

            rankings.add(
                    new MonthlyReportResponse.Ranking(
                            getCityName(airportCode),
                            (int) count,
                            percentage
                    )
            );
        }

        rankings.sort(
                Comparator.comparingInt(
                                MonthlyReportResponse
                                        .Ranking::percentage
                        )
                        .reversed()
        );

        String insightMessage;

        if (rankings.isEmpty()) {

            insightMessage =
                    "이번 달 외출 자외선 노출 기록이 없습니다.";

        } else {

            MonthlyReportResponse.Ranking top =
                    rankings.get(0);

            insightMessage =
                    top.route()
                            + " 노선의 자외선 노출 비중이 "
                            + top.percentage()
                            + "%로 가장 높았어요.";
        }

        return new MonthlyReportResponse.RouteRanking(
                insightMessage,
                rankings
        );
    }


    // =========================
    // DAILY EXPOSURE
    // =========================

    private List<MonthlyReportResponse.DailyExposure>
    createDailyExposure(
            YearMonth yearMonth,
            List<ExposureRecord> records
    ) {

        List<MonthlyReportResponse.DailyExposure> result =
                new ArrayList<>();

        for (int day = 1;
             day <= yearMonth.lengthOfMonth();
             day++) {

            int currentDay = day;

            List<ExposureRecord> dayRecords =
                    records.stream()
                            .filter(record ->
                                    record.getDate()
                                            .getDayOfMonth()
                                            == currentDay
                            )
                            .toList();

            double outingScore =
                    dayRecords.stream()
                            .filter(ExposureRecord::isOuting)
                            .map(ExposureRecord::getEstimatedExposureScore)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

            double indoorScore =
                    dayRecords.stream()
                            .filter(record ->
                                    !record.isOuting()
                            )
                            .map(ExposureRecord::getEstimatedExposureScore)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

            result.add(
                    new MonthlyReportResponse.DailyExposure(
                            day,
                            (int) Math.round(outingScore),
                            (int) Math.round(indoorScore)
                    )
            );
        }

        return result;
    }


    // =========================
    // TREND
    // =========================

    private MonthlyReportResponse.Trend createTrend(
            User user,
            YearMonth currentMonth
    ) {

        List<MonthlyReportResponse.MonthValue> months =
                new ArrayList<>();

        List<Double> values =
                new ArrayList<>();

        for (int i = 2; i >= 0; i--) {

            YearMonth target =
                    currentMonth.minusMonths(i);

            List<ExposureRecord> records =
                    exposureRecordRepository
                            .findByUserAndDateBetweenOrderByDateAsc(
                                    user,
                                    target.atDay(1),
                                    target.atEndOfMonth()
                            );

            double score =
                    records.stream()
                            .filter(ExposureRecord::isOuting)
                            .map(ExposureRecord::getEstimatedExposureScore)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

            values.add(score);

            months.add(
                    new MonthlyReportResponse.MonthValue(
                            target.getMonthValue(),
                            (int) Math.round(score)
                    )
            );
        }

        double previous =
                values.get(0);

        double current =
                values.get(2);

        String comparisonText;

        if (previous <= 0) {

            comparisonText =
                    "이전 비교 데이터가 없습니다.";

        } else {

            int percentage =
                    (int) Math.round(
                            ((current - previous)
                                    / previous)
                                    * 100
                    );

            String sign =
                    percentage >= 0
                            ? "+"
                            : "";

            comparisonText =
                    currentMonth
                            .minusMonths(2)
                            .getMonthValue()
                            + "월 대비 "
                            + sign
                            + percentage
                            + "% 변화";
        }

        return new MonthlyReportResponse.Trend(
                comparisonText,
                months
        );
    }


    // =========================
    // ANALYSIS
    // =========================

    private MonthlyReportResponse.Analysis createAnalysis(
            List<ExposureRecord> records
    ) {

        ExposureRecord strongest =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .max(
                                Comparator.comparingDouble(
                                        record ->
                                                Optional.ofNullable(
                                                        record.getEstimatedExposureScore()
                                                ).orElse(0.0)
                                )
                        )
                        .orElse(null);

        MonthlyReportResponse.AnalysisItem strongestDay;

        if (strongest == null) {

            strongestDay =
                    new MonthlyReportResponse.AnalysisItem(
                            "기록 없음",
                            "이번 달 자외선 노출 기록이 없습니다.",
                            null
                    );

        } else {

            String strongestTag =
                    switch (strongest.getRiskLevel()) {
                        case DANGER -> "위험";
                        case CAUTION -> "주의";
                        case SAFE -> "안전";
                    };

            strongestDay =
                    new MonthlyReportResponse.AnalysisItem(
                            strongest.getDate().getMonthValue()
                                    + "/"
                                    + strongest.getDate().getDayOfMonth()
                                    + " "
                                    + getCityName(
                                    strongest.getAirportCode()
                            ),

                            "서울 기준 약 "
                                    + roundOne(
                                    strongest.getKoreaComparison()
                            )
                                    + "배 수준, UV 지수 "
                                    + strongest.getUvIndex(),

                            strongestTag
                    );
        }

        List<ExposureRecord> missed =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .filter(record ->
                                record.getRiskLevel()
                                        == RiskLevel.DANGER
                        )
                        .filter(record ->
                                record.getSchedule() != null
                                        && !record.getSchedule()
                                        .isApplied()
                        )
                        .toList();

        String missedTitle =
                missed.isEmpty()
                        ? "없음"
                        : missed.stream()
                        .map(record ->
                             record.getDate()
                                     .getMonthValue()
                             + "/"
                             + record.getDate()
                                     .getDayOfMonth()
                        )
                        .distinct()
                        .collect(
                                Collectors.joining(", ")
                        );

        MonthlyReportResponse.AnalysisItem missedDays =
                new MonthlyReportResponse.AnalysisItem(
                        missedTitle,
                        missed.isEmpty()
                                ? "위험한 날의 미대응 기록이 없습니다."
                                : "위험한 날이었지만 제품 선택 기록이 없습니다.",
                        null
                );

        List<ExposureRecord> good =
                records.stream()
                        .filter(ExposureRecord::isOuting)
                        .filter(record ->
                                record.getSchedule() != null
                                        && record.getSchedule()
                                        .isApplied()
                        )
                        .toList();

        String goodTitle =
                good.isEmpty()
                        ? "없음"
                        : good.stream()
                        .map(record ->
                             record.getDate()
                                     .getMonthValue()
                             + "/"
                             + record.getDate()
                                     .getDayOfMonth()
                        )
                        .distinct()
                        .collect(
                                Collectors.joining(", ")
                        );

        MonthlyReportResponse.AnalysisItem goodDays =
                new MonthlyReportResponse.AnalysisItem(
                        goodTitle,
                        good.isEmpty()
                                ? "아직 자외선 대응 기록이 없습니다."
                                : "자외선 환경에 맞춰 제품을 선택하고 대응했어요.",
                        good.isEmpty()
                                ? null
                                : "대응 완료"
                );

        return new MonthlyReportResponse.Analysis(
                strongestDay,
                missedDays,
                goodDays
        );
    }


    // =========================
    // NEXT MONTH
    // =========================

    private MonthlyReportResponse.NextMonthForecast
    createNextMonthForecast(
            User user,
            YearMonth currentMonth,
            List<ExposureRecord> currentRecords
    ) {

        YearMonth nextMonth =
                currentMonth.plusMonths(1);

        LocalDateTime start =
                nextMonth.atDay(1)
                        .atStartOfDay();

        LocalDateTime end =
                nextMonth.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay()
                        .minusNanos(1);

        List<Schedule> schedules =
                scheduleRepository
                        .findByUserAndDepartureTimeBetweenOrderByDepartureTimeAsc(
                                user,
                                start,
                                end
                        );

        Map<String, Long> routeCounts =
                schedules.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Schedule::getArrivalAirport,
                                        Collectors.counting()
                                )
                        );

        List<String> scheduledRoutes =
                routeCounts.entrySet()
                        .stream()
                        .sorted(
                                Map.Entry
                                        .<String, Long>comparingByValue()
                                        .reversed()
                        )
                        .map(entry ->
                                getCityName(
                                        entry.getKey()
                                )
                                        + " "
                                        + entry.getValue()
                                        + "회"
                        )
                        .toList();

        long currentScheduleCount =
                currentRecords.stream()
                        .map(record ->
                                record.getSchedule()
                                        .getId()
                        )
                        .distinct()
                        .count();

        double multiplier;

        if (currentScheduleCount == 0) {
            multiplier = 0.0;
        } else {
            multiplier =
                    roundOne(
                            schedules.size()
                                    / (double) currentScheduleCount
                    );
        }

        String recoveryPeriod =
                findLongestRecoveryPeriod(
                        nextMonth,
                        schedules,
                        String.valueOf(user.getBaseAirport())
                );

        String tip;


        if (recoveryPeriod == null) {

            tip =
                    "다음 달 일정 사이 충분한 대기 기간이 확인되지 않았어요.";

        } else {

            tip =
                    recoveryPeriod
                            + " 기간을 피부 회복 일정으로 활용할 수 있어요.";
        }

        return new MonthlyReportResponse.NextMonthForecast(
                multiplier,
                scheduledRoutes,
                recoveryPeriod,
                tip
        );

    }


    // =========================
    // CLINIC
    // =========================

    private MonthlyReportResponse.Clinic createClinic(
            User user,
            YearMonth currentMonth,
            MonthlyReportResponse.NextMonthForecast forecast
    ) {

        // 조회 월 포함 최근 3개월
        // 예: 2026-08 조회 → 6/1 ~ 8/31
        LocalDate threeMonthsStart =
                currentMonth
                        .minusMonths(2)
                        .atDay(1);

        LocalDate threeMonthsEnd =
                currentMonth
                        .atEndOfMonth();

        List<ExposureRecord> threeMonthRecords =
                exposureRecordRepository
                        .findByUserAndDateBetweenOrderByDateAsc(
                                user,
                                threeMonthsStart,
                                threeMonthsEnd
                        );

        double cumulativeComparison =
                threeMonthRecords.stream()
                        .filter(ExposureRecord::isOuting)
                        .map(ExposureRecord::getKoreaComparison)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum();

        int exposurePercentage =
                (int) Math.min(
                        100,
                        Math.round(
                                cumulativeComparison*10
                        )
                );

        String level;

        if (exposurePercentage >= 75) {
            level = "상위 구간";
        } else if (exposurePercentage >= 50) {
            level = "중간 구간";
        } else {
            level = "낮은 구간";
        }

        String description =
                "최근 3개월 누적 자외선 노출 수준이 "
                        + level
                        + "으로 분석되었습니다.";

        if (forecast.recoveryPeriod() != null) {

            description +=
                    " "
                            + forecast.recoveryPeriod()
                            + " 기간이 있어 피부 회복 시간을 확보할 수 있습니다.";
        }

        return new MonthlyReportResponse.Clinic(
                level,
                exposurePercentage,
                description,
                "https://amredclinic.com/ko"
        );
    }


    // =========================
    // HELPERS
    // =========================

    private String getCityName(
            String airportCode
    ) {

        try {

            return AirportLocationMapper
                    .getAirportInfo(
                            airportCode
                    )
                    .city();

        } catch (Exception e) {

            return airportCode;
        }
    }

    private double roundOne(
            Double value
    ) {

        if (value == null) {
            return 0.0;
        }

        return Math.round(
                value * 10.0
        ) / 10.0;
    }

    private String findLongestRecoveryPeriod(
            YearMonth month,
            List<Schedule> schedules,
            String baseAirport
    ) {

        String airportName =
                getAirportDisplayName(baseAirport);

        if (schedules.isEmpty()) {

            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();

            return formatRecoveryPeriod(
                    start,
                    end,
                    airportName
            );
        }

        List<LocalDate> flightDates =
                schedules.stream()
                        .flatMap(schedule ->
                                List.of(
                                        schedule.getDepartureTime()
                                                .toLocalDate(),
                                        schedule.getArrivalTime()
                                                .toLocalDate()
                                ).stream()
                        )
                        .distinct()
                        .sorted()
                        .toList();

        LocalDate monthStart =
                month.atDay(1);

        LocalDate monthEnd =
                month.atEndOfMonth();

        LocalDate bestStart = null;
        LocalDate bestEnd = null;

        LocalDate currentStart =
                monthStart;

        for (LocalDate flightDate : flightDates) {

            LocalDate gapEnd =
                    flightDate.minusDays(1);

            if (!gapEnd.isBefore(currentStart)) {

                if (bestStart == null
                        || daysBetween(
                        currentStart,
                        gapEnd
                ) > daysBetween(
                        bestStart,
                        bestEnd
                )) {

                    bestStart = currentStart;
                    bestEnd = gapEnd;
                }
            }

            currentStart =
                    flightDate.plusDays(1);
        }

        if (!currentStart.isAfter(monthEnd)) {

            if (bestStart == null
                    || daysBetween(
                    currentStart,
                    monthEnd
            ) > daysBetween(
                    bestStart,
                    bestEnd
            )) {

                bestStart = currentStart;
                bestEnd = monthEnd;
            }
        }

        if (bestStart == null) {
            return null;
        }

        return formatRecoveryPeriod(
                bestStart,
                bestEnd,
                airportName
        );
    }

    private long daysBetween(
            LocalDate start,
            LocalDate end
    ) {

        return java.time.temporal.ChronoUnit
                .DAYS
                .between(
                        start,
                        end
                ) + 1;
    }
    private String formatRecoveryPeriod(
            LocalDate start,
            LocalDate end,
            String airportName
    ) {

        if (start.getMonthValue()
                == end.getMonthValue()) {

            return start.getMonthValue()
                    + "/"
                    + start.getDayOfMonth()
                    + "~"
                    + end.getDayOfMonth()
                    + " "
                    + airportName
                    + " 대기";
        }

        return start.getMonthValue()
                + "/"
                + start.getDayOfMonth()
                + "~"
                + end.getMonthValue()
                + "/"
                + end.getDayOfMonth()
                + " "
                + airportName
                + " 대기";
    }

    private String getAirportDisplayName(
            String airportCode
    ) {

        if (airportCode == null) {
            return "국내";
        }

        return switch (airportCode.toUpperCase()) {
            case "ICN", "INCHEON" -> "인천";
            case "GMP", "GIMPO" -> "김포";
            default -> airportCode;
        };
    }

}