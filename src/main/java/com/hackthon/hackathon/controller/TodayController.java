package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.SolutionAiResponse;
import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.dto.home.HomeUvResponse;
import com.hackthon.hackathon.dto.today.TodayResponse;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.UserRepository;
import com.hackthon.hackathon.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.hackthon.hackathon.dto.today.TodayOutingRequest;
import com.hackthon.hackathon.dto.today.TodayOutingResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TodayController {

    private static final int DEFAULT_FLIGHT_EXPOSURE_MINUTES = 30;

    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final SunlightWindowService sunlightWindowService;
    private final ExposureCalculationService exposureCalculationService;
    private final HomeUvService homeUvService;
    private final SolutionAiService solutionAiService;
    private final TodayService todayService;
    private final ExposureRecordService exposureRecordService;
    private final TodayOutingService todayOutingService;

    @GetMapping("/today")
    public ResponseEntity<TodayResponse> getToday() {

        Long userId = 1L;

        // =========================
        // 1. USER
        // =========================

        User user =
                userRepository.findById(userId)
                        .orElse(null);

        if (user == null) {

            return ResponseEntity.ok(
                    TodayResponse.guest()
            );
        }


        // =========================
        // 2. 오늘 위치 / 일정
        // =========================

        TodayService.TodayScheduleInfo scheduleInfo =
                todayService.getTodayScheduleInfo(
                        userId
                );

        String airportCode =
                scheduleInfo.airportCode();


        // =========================
        // 3. 현재 위치 날씨
        // =========================

        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );


        // =========================
        // 4. 외출 가능 시간
        // =========================

        SunlightWindowService.AvailableWindow availableWindow;

        if (scheduleInfo.baseDay()) {

            /*
             * 일정 없는 소속공항 대기일
             *
             * 하루 전체를 기준으로 잡고
             * 아래에서 실제 일출~일몰과 교집합 계산
             */
            availableWindow =
                    sunlightWindowService
                            .calculateBaseDayAvailableWindow(
                                    scheduleInfo.localDate()
                            );

        } else {

            /*
             * 기존 비행/레이오버 일정
             */
            availableWindow =
                    sunlightWindowService
                            .calculateAvailableWindow(
                                    scheduleInfo.arrivalTime(),
                                    scheduleInfo.nextDepartureTime(),
                                    scheduleInfo.quickTurn()
                            )
                            .orElse(null);
        }


        // =========================
        // 5. 햇빛창
        // =========================

        List<SunlightWindowService.SunlightWindow> windows;

        if (availableWindow == null) {

            windows = List.of();

        } else {

            windows =
                    sunlightWindowService
                            .calculateSunlightWindows(
                                    availableWindow,
                                    weather
                            );
        }


        // =========================
        // 6. 서울 비교
        // =========================

        WeatherResponse seoulWeather =
                weatherService.getSeoulWeather();

        int sunlightMinutes =
                windows.stream()
                        .mapToInt(window ->
                                (int) window.minutes()
                        )
                        .sum();

        double seoulComparableExposureScore =
                exposureCalculationService
                        .calculateSeoulComparableExposureScore(
                                seoulWeather,
                                sunlightMinutes
                        );

        ExposureCalculationService.ExposureResult exposureResult =
                exposureCalculationService.calculateExposure(
                        windows,
                        weather,
                        seoulComparableExposureScore
                );


        // =========================
        // 7. 홈 UV 데이터
        // =========================

        HomeUvResponse homeResponse =
                homeUvService.createTestHomeUv(
                        userId,
                        airportCode,
                        exposureResult,
                        windows
                );


        // =========================
        // 8. ExposureRecord 저장
        // =========================

        if (scheduleInfo.baseDay()) {

            /*
             * 일정 없는 날
             *
             * schedule = null
             * User + 날짜 기준으로 저장
             */
            exposureRecordService
                    .saveOrUpdateBaseDay(
                            user,
                            airportCode,
                            scheduleInfo.localDate(),
                            scheduleInfo.outing(),
                            exposureResult,
                            homeResponse.weatherCondition()
                    );

        } else {

            Schedule currentSchedule =
                    scheduleInfo.schedule();

            if (currentSchedule != null) {

                exposureRecordService
                        .saveOrUpdate(
                                currentSchedule,
                                airportCode,
                                scheduleInfo.localDate(),
                                exposureResult,
                                homeResponse.weatherCondition()
                        );
            }
        }


        // =========================
        // 9. 사용자 정보
        // =========================

        String position;

        if (scheduleInfo.baseDay()) {

            position = "대기";

        } else if (scheduleInfo.quickTurn()) {

            position = "퀵턴";

        } else {

            position = "레이오버";
        }

        TodayResponse.UserInfo userInfo =
                new TodayResponse.UserInfo(
                        user.getName(),
                        position
                );


        // =========================
        // 10. 위치
        // =========================

        TodayResponse.LocationInfo location =
                new TodayResponse.LocationInfo(
                        homeResponse.city(),
                        homeResponse.country(),
                        exposureResult.riskLevel()
                );


        // =========================
        // 11. UV GRAPH
        // =========================

        List<TodayResponse.UvPoint> uvGraph =
                homeResponse.uvGraph()
                        .stream()
                        .map(point ->
                                new TodayResponse.UvPoint(
                                        point.time(),
                                        point.uvValue()
                                )
                        )
                        .toList();


        // =========================
        // 12. 날씨
        // =========================

        TodayResponse.Weather todayWeather =
                new TodayResponse.Weather(
                        convertWeatherCondition(
                                homeResponse.weatherCondition()
                        ),
                        homeResponse.temperature()
                );


        /*
         * 일정 없는 날이면
         * 비행 노출 시간은 0
         */
        int flightExposureMinutes =
                scheduleInfo.baseDay()
                        ? 0
                        : DEFAULT_FLIGHT_EXPOSURE_MINUTES;

        double koreaComparison =
                Math.round(
                        homeResponse.koreaComparison()
                                * 10.0
                ) / 10.0;

        int koreaEquivalentMinutes =
                (int) Math.round(
                        flightExposureMinutes
                                * koreaComparison
                );


        TodayResponse.UvSummary uvSummary =
                new TodayResponse.UvSummary(
                        homeResponse.city(),
                        homeResponse.uvIndex(),
                        koreaComparison,
                        flightExposureMinutes,
                        koreaEquivalentMinutes,
                        todayWeather,
                        uvGraph
                );


        // =========================
        // 13. INDOOR
        // =========================

        if (!scheduleInfo.outing()) {

            TodayResponse response =
                    new TodayResponse(
                            "INDOOR",
                            userInfo,
                            location,
                            homeResponse.currentTime(),
                            uvSummary,
                            null,
                            List.of()
                    );

            return ResponseEntity.ok(
                    response
            );
        }


        // =========================
        // 14. OUTING AI
        // =========================

        SolutionAiResponse solution =
                solutionAiService.generateSolution(
                        userId,
                        homeResponse.uvIndex(),
                        homeResponse.sunlightMinutes(),
                        homeResponse.weatherCondition()
                );


        // =========================
        // 15. PRODUCTS
        // =========================

        List<TodayResponse.Product> products =
                homeResponse.sunscreens()
                        .stream()
                        .map(item ->
                                new TodayResponse.Product(
                                        item.sunscreenId(),
                                        item.name(),

                                        switch (item.filterType()) {
                                            case PHYSICAL -> "무기자차";
                                            case ORGANIC -> "유기자차";
                                            case MIXED -> "혼합자차";
                                        },

                                        Integer.parseInt(
                                                item.displayedSpf()
                                                        .replaceAll(
                                                                "[^0-9]",
                                                                ""
                                                        )
                                        ),

                                        item.sunscreenId()
                                                .equals(
                                                        solution.sunscreenId()
                                                )
                                )
                        )
                        .toList();


        // =========================
        // 16. TAGS
        // =========================

        List<String> tags =
                new ArrayList<>();

        switch (homeResponse.weatherCondition()) {

            case "맑음" ->
                    tags.add("맑은 날");

            case "비" ->
                    tags.add("비오는 날");

            case "눈" ->
                    tags.add("눈오는 날");

            case "흐림" ->
                    tags.add("흐린 날");

            case "안개" ->
                    tags.add("안개 낀 날");
        }

        if (homeResponse.uvIndex() < 3) {

            tags.add("자외선 약함");

        } else if (homeResponse.uvIndex() < 6) {

            tags.add("자외선 보통");

        } else {

            tags.add("자외선 강함");
        }


        // =========================
        // 17. 선크림 추천
        // =========================

        TodayResponse.SunProtection sunProtection =
                new TodayResponse.SunProtection(
                        tags,
                        products,
                        solution.message()
                );


        // =========================
        // 18. AI SOLUTIONS
        // =========================

        List<TodayResponse.Solution> todaySolutions =
                solution.solutions()
                        .stream()
                        .map(item ->
                                new TodayResponse.Solution(
                                        item.phase(),
                                        item.title(),
                                        item.description()
                                )
                        )
                        .toList();


        // =========================
        // 19. RESPONSE
        // =========================

        TodayResponse response =
                new TodayResponse(
                        "OUTING",
                        userInfo,
                        location,
                        homeResponse.currentTime(),
                        uvSummary,
                        sunProtection,
                        todaySolutions
                );

        return ResponseEntity.ok(
                response
        );
    }

    @PatchMapping("/today/outing")
    public ResponseEntity<TodayOutingResponse> updateTodayOuting(
            @RequestBody TodayOutingRequest request
    ) {

        Long userId = 1L;

        return ResponseEntity.ok(
                todayOutingService.updateOuting(
                        userId,
                        request.outing()
                )
        );
    }

    private String convertWeatherCondition(
            String condition
    ) {

        return switch (condition) {

            case "맑음" -> "CLEAR";
            case "흐림" -> "CLOUDY";
            case "비" -> "RAIN";
            case "눈" -> "SNOW";
            case "안개" -> "FOG";
            case "뇌우" -> "THUNDERSTORM";

            default -> "UNKNOWN";
        };
    }
}