package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.SolutionAiResponse;
import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.dto.home.HomeUvResponse;
import com.hackthon.hackathon.dto.today.TodayResponse;
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

    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final SunlightWindowService sunlightWindowService;
    private final ExposureCalculationService exposureCalculationService;
    private final HomeUvService homeUvService;
    private final SolutionAiService solutionAiService;
    private final TodayService todayService;

    @GetMapping("/today")
    public ResponseEntity<TodayResponse> getToday() {

        Long userId = 1L;

        // 1. 유저 없으면 GUEST
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.ok(TodayResponse.guest());
        }

        // 2. DB 일정 조회
        TodayService.TodayScheduleInfo scheduleInfo =
                todayService.getTodayScheduleInfo(userId);

        String airportCode = scheduleInfo.airportCode();

        // 3. 현재 체류지 날씨
        WeatherResponse weather =
                weatherService.getWeather(airportCode);

        // 4. 외출 가능 시간
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

        // 5. 햇빛창 계산
        List<SunlightWindowService.SunlightWindow> windows =
                sunlightWindowService.calculateSunlightWindows(
                        availableWindow,
                        weather
                );

        // 6. 서울 기준 노출점수
        double seoulDailyAverageExposureScore = 8.0;

        ExposureCalculationService.ExposureResult exposureResult =
                exposureCalculationService.calculateExposure(
                        windows,
                        weather,
                        seoulDailyAverageExposureScore
                );

        // 7. 홈 UV 데이터
        HomeUvResponse homeResponse =
                homeUvService.createTestHomeUv(
                        userId,
                        airportCode,
                        exposureResult,
                        windows
                );

        // 8. AI 솔루션
        SolutionAiResponse solution =
                solutionAiService.generateSolution(
                        userId,
                        homeResponse.uvIndex(),
                        homeResponse.sunlightMinutes(),
                        homeResponse.weatherCondition()
                );

        // 9. 선크림 목록
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
                                                        .replaceAll("[^0-9]", "")
                                        ),

                                        item.sunscreenId()
                                                .equals(solution.sunscreenId())
                                )
                        )
                        .toList();

        // 10. 태그
        List<String> tags = new ArrayList<>();

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

        // 11. 선크림 추천
        TodayResponse.SunProtection sunProtection =
                new TodayResponse.SunProtection(
                        tags,
                        products,
                        solution.message()
                );

        // 12. AI 3단계 솔루션
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

        // 13. 유저 정보
        String position =
                scheduleInfo.quickTurn()
                        ? "퀵턴"
                        : "레이오버";

        TodayResponse.UserInfo userInfo =
                new TodayResponse.UserInfo(
                        user.getName(),
                        position
                );

        // 14. 위치
        TodayResponse.LocationInfo location =
                new TodayResponse.LocationInfo(
                        homeResponse.city(),
                        homeResponse.country()
                );

        // 15. UV 그래프
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

        // 16. 날씨
        TodayResponse.Weather todayWeather =
                new TodayResponse.Weather(
                        convertWeatherCondition(
                                homeResponse.weatherCondition()
                        ),
                        homeResponse.temperature()
                );

        // 17. 디자인 기준 비행 노출시간
        int flightExposureMinutes = 30;

        // 서울 기준 환산 시간
        int koreaEquivalentMinutes =
                (int) Math.round(
                        flightExposureMinutes
                                * homeResponse.koreaComparison()
                );

        // 18. UV 요약
        TodayResponse.UvSummary uvSummary =
                new TodayResponse.UvSummary(
                        homeResponse.city(),
                        homeResponse.uvIndex(),
                        homeResponse.koreaComparison(),
                        flightExposureMinutes,
                        koreaEquivalentMinutes,
                        todayWeather,
                        uvGraph
                );

        // 19. 외출 여부에 따른 mode
        String mode =
                scheduleInfo.outing()
                        ? "OUTING"
                        : "INDOOR";

        // 20. 최종 응답
        TodayResponse response =
                new TodayResponse(
                        mode,
                        userInfo,
                        location,
                        homeResponse.currentTime(),
                        uvSummary,
                        sunProtection,
                        todaySolutions
                );

        return ResponseEntity.ok(response);
    }

    private String convertWeatherCondition(String condition) {

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