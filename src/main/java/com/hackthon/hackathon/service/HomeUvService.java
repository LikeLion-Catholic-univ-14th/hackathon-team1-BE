package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.dto.home.HomeUvResponse;
import com.hackthon.hackathon.dto.home.UvGraphPoint;
import com.hackthon.hackathon.util.AirportInfo;
import com.hackthon.hackathon.util.AirportLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HomeUvService {

    private final WeatherService weatherService;
    private final TimeConversionService timeConversionService;
    private final ExposureCalculationService exposureCalculationService;
    private final UserSunscreenService userSunscreenService;

    public HomeUvResponse createTestHomeUv(
            Long userId,
            String airportCode,
            ExposureCalculationService.ExposureResult result,
            List<SunlightWindowService.SunlightWindow> windows
    ) {

        WeatherResponse weather =
                weatherService.getWeather(airportCode);

        AirportInfo airport =
                AirportLocationMapper.getAirportInfo(airportCode);

        String weatherCondition =
                exposureCalculationService.calculateWeatherCondition(
                        windows,
                        weather
                );

        int currentHourIndex =
                findCurrentHourIndex(
                        weather,
                        airportCode
                );

        if ("알 수 없음".equals(weatherCondition)
                && currentHourIndex >= 0) {

            Integer weatherCode =
                    weather.getHourly()
                            .getWeatherCode()
                            .get(currentHourIndex);

            weatherCondition =
                    convertWeatherCode(
                            weatherCode
                    );
        }

        Instant now = Instant.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "hh:mm a",
                        Locale.ENGLISH
                );

        String currentTime =
                timeConversionService
                        .utcToLocal(now, airportCode)
                        .format(formatter);

        String koreaTime =
                timeConversionService
                        .utcToKorea(now)
                        .format(formatter);

        List<UvGraphPoint> uvGraph =
                createTodayUvGraph(weather, airportCode);
        double todayMaxUv = uvGraph.stream()
                .mapToDouble(UvGraphPoint::uvValue)
                .max()
                .orElse(0.0);

        int displayTemperature =
                result.temperature();

        if (windows.isEmpty()
                && currentHourIndex >= 0) {

            Double currentTemperature =
                    weather.getHourly()
                            .getTemperature()
                            .get(currentHourIndex);

            if (currentTemperature != null) {
                displayTemperature =
                        (int) Math.round(
                                currentTemperature
                        );
            }
        }
        List<UserSunscreenService.SunscreenProtectionResponse> sunscreens =
                userSunscreenService.calculateUserSunscreens(
                        userId,
                        todayMaxUv
                );
        UserSunscreenService.SunscreenProtectionResponse recommendedSunscreen =
                userSunscreenService.recommendSunscreen(
                        userId,
                        todayMaxUv
                );

        return new HomeUvResponse(
                airport.city(),
                airport.country(),
                currentTime,
                koreaTime,

                todayMaxUv,
                result.koreaComparison(),

                result.sunlightMinutes(),
                displayTemperature,
                weatherCondition,
                exposureCalculationService
                        .calculateRiskLevel(
                                todayMaxUv
                        ),

                uvGraph,
                sunscreens
        );
    }

    private int findCurrentHourIndex(
            WeatherResponse weather,
            String airportCode
    ) {
        if (weather == null
                || weather.getHourly() == null
                || weather.getHourly().getTime() == null) {
            return -1;
        }

        LocalDateTime localNow =
                timeConversionService
                        .utcToLocal(
                                Instant.now(),
                                airportCode
                        )
                        .toLocalDateTime();

        List<String> times =
                weather.getHourly().getTime();

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime time =
                    LocalDateTime.parse(
                            times.get(i)
                    );

            if (time.toLocalDate()
                    .equals(localNow.toLocalDate())
                    && time.getHour() == localNow.getHour()) {
                return i;
            }
        }

        return -1;
    }

    private String convertWeatherCode(
            Integer weatherCode
    ) {
        if (weatherCode == null) {
            return "알 수 없음";
        }

        if (weatherCode == 0) {
            return "맑음";
        }

        if (weatherCode >= 1 && weatherCode <= 3) {
            return "흐림";
        }

        if (weatherCode >= 45 && weatherCode <= 48) {
            return "안개";
        }

        if ((weatherCode >= 51 && weatherCode <= 67)
                || (weatherCode >= 80 && weatherCode <= 82)) {
            return "비";
        }

        if ((weatherCode >= 71 && weatherCode <= 77)
                || (weatherCode >= 85 && weatherCode <= 86)) {
            return "눈";
        }

        if (weatherCode >= 95) {
            return "뇌우";
        }

        return "기타";
    }

    private List<UvGraphPoint> createTodayUvGraph(
            WeatherResponse weather,
            String airportCode
    ) {

        List<UvGraphPoint> graph = new ArrayList<>();

        List<String> times =
                weather.getHourly().getTime();

        List<Double> uvIndexes =
                weather.getHourly().getUvIndex();

        LocalDate localToday =
                timeConversionService
                        .utcToLocal(
                                Instant.now(),
                                airportCode
                        )
                        .toLocalDate();

        for (int i = 0; i < times.size(); i++) {

            LocalDateTime time =
                    LocalDateTime.parse(times.get(i));

            if (!time.toLocalDate().equals(localToday)) {
                continue;
            }

            Double uv = uvIndexes.get(i);

            if (uv == null) {
                continue;
            }

            graph.add(
                    new UvGraphPoint(
                            time.toLocalTime().toString(),
                            uv
                    )
            );
        }

        return graph;
    }
}
