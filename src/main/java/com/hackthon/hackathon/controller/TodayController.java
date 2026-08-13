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
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/today")
    public ResponseEntity<TodayResponse> getToday() {

        Long userId = 1L;

        // 1. 유저 없으면 GUEST
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.ok(
                    TodayResponse.guest()
            );
        }

        // 2. 현재 체류 스케줄 조회
        TodayService.TodayScheduleInfo scheduleInfo =
                todayService.getTodayScheduleInfo(
                        userId
                );

        String airportCode =
                scheduleInfo.airportCode();

        // 3. 현재 체류지 날씨
        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );

        // 4. 외출 가능 시간 계산
        SunlightWindowService.AvailableWindow availableWindow =
                sunlightWindowService.calculateAvailableWindow(
                                scheduleInfo.arrivalTime(),
                                scheduleInfo.nextDepartureTime(),
                                scheduleInfo.quickTurn()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "외출 가능 시간이 없습니다."
                                )
                        );

        // 5. 실제 일출/일몰과 겹치는 햇빛창 계산
        List<SunlightWindowService.SunlightWindow> windows =
                sunlightWindowService.calculateSunlightWindows(
                        availableWindow,
                        weather
                );

        // 6. 서울 동일 노출시간 기준 비교값 계산
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

        // 7. 홈 UV 기본 데이터
        HomeUvResponse homeResponse =
                homeUvService.createTestHomeUv(
                        userId,
                        airportCode,
                        exposureResult,
                        windows
                );
        // 8. 노출 기록 저장 또는 갱신
        Schedule currentSchedule =
                todayService.getCurrentSchedule(
                        userId
                );

        exposureRecordService.saveOrUpdate(
                currentSchedule,
                airportCode,
                currentSchedule.getArrivalTime()
                        .toLocalDate(),
                exposureResult,
                homeResponse.weatherCondition()
        );

        // 공통 응답 데이터 생성
        TodayResponse.UserInfo userInfo =
                new TodayResponse.UserInfo(
                        user.getName(),
                        scheduleInfo.quickTurn()
                                ? "퀵턴"
                                : "레이오버"
                );

        TodayResponse.LocationInfo location =
                new TodayResponse.LocationInfo(
                        homeResponse.city(),
                        homeResponse.country()
                );

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

        TodayResponse.Weather todayWeather =
                new TodayResponse.Weather(
                        convertWeatherCondition(
                                homeResponse.weatherCondition()
                        ),
                        homeResponse.temperature()
                );

        int flightExposureMinutes =
                DEFAULT_FLIGHT_EXPOSURE_MINUTES;

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

        // 8. 실내 모드
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

        // 9. OUTING일 때만 AI 솔루션 생성
        SolutionAiResponse solution =
                solutionAiService.generateSolution(
                        userId,
                        homeResponse.uvIndex(),
                        homeResponse.sunlightMinutes(),
                        homeResponse.weatherCondition()
                );

        // 10. 선크림 목록
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

        // 11. 태그
        List<String> tags =
                new ArrayList<>();

        switch (homeResponse.weatherCondition()) {
            case "맑음" -> tags.add("맑은 날");
            case "비" -> tags.add("비오는 날");
            case "눈" -> tags.add("눈오는 날");
            case "흐림" -> tags.add("흐린 날");
            case "안개" -> tags.add("안개 낀 날");
        }

        if (homeResponse.uvIndex() < 3) {
            tags.add("자외선 약함");
        } else if (homeResponse.uvIndex() < 6) {
            tags.add("자외선 보통");
        } else {
            tags.add("자외선 강함");
        }

        // 12. 선크림 추천 영역
        TodayResponse.SunProtection sunProtection =
                new TodayResponse.SunProtection(
                        tags,
                        products,
                        solution.message()
                );

        // 13. AI 3단계 솔루션
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

        // 14. OUTING 최종 응답
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