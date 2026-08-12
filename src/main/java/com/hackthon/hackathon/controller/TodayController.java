package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.dto.home.HomeUvResponse;
import com.hackthon.hackathon.service.ExposureCalculationService;
import com.hackthon.hackathon.service.HomeUvService;
import com.hackthon.hackathon.service.SunlightWindowService;
import com.hackthon.hackathon.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TodayController {

    private final WeatherService weatherService;
    private final SunlightWindowService sunlightWindowService;
    private final ExposureCalculationService exposureCalculationService;
    private final HomeUvService homeUvService;

    @GetMapping("/today")
    public ResponseEntity<HomeUvResponse> testHome() {

        Long userId = 1L;
        String airportCode = "SYD";

        WeatherResponse weather =
                weatherService.getWeather(airportCode);

        LocalDateTime arrivalTime =
                LocalDateTime.of(2026, 8, 12, 0, 0);

        LocalDateTime nextDepartureTime =
                LocalDateTime.of(2026, 8, 14, 10, 0);

        SunlightWindowService.AvailableWindow availableWindow =
                sunlightWindowService.calculateAvailableWindow(
                        arrivalTime,
                        nextDepartureTime,
                        false
                ).orElseThrow();

        List<SunlightWindowService.SunlightWindow> windows =
                sunlightWindowService.calculateSunlightWindows(
                        availableWindow,
                        weather
                );

        double testSeoulDailyAverageExposureScore = 8.0;

        ExposureCalculationService.ExposureResult exposureResult =
                exposureCalculationService.calculateExposure(
                        windows,
                        weather,
                        testSeoulDailyAverageExposureScore
                );

        HomeUvResponse response =
                homeUvService.createTestHomeUv(
                        userId,
                        airportCode,
                        exposureResult,
                        windows
                );

        return ResponseEntity.ok(response);
    }
}