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
                result.temperature(),
                weatherCondition,
                result.riskLevel(),

                uvGraph,
                sunscreens
        );
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