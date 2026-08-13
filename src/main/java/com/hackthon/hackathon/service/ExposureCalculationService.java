package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.enums.RiskLevel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExposureCalculationService {

    public UvSummary calculateUvSummary(
            List<SunlightWindowService.SunlightWindow> windows,
            WeatherResponse weather
    ) {

        List<Double> uvValues = new ArrayList<>();
        List<Double> temperatures = new ArrayList<>();

        List<String> times = weather.getHourly().getTime();
        List<Double> uvIndexes = weather.getHourly().getUvIndex();
        List<Double> temperatureValues =
                weather.getHourly().getTemperature();

        for (int i = 0; i < times.size(); i++) {

            LocalDateTime time =
                    LocalDateTime.parse(times.get(i));

            for (SunlightWindowService.SunlightWindow window : windows) {

                boolean included =
                        overlapsHourlyInterval(
                                time,
                                window
                        );

                if (included) {

                    Double uv = uvIndexes.get(i);
                    Double temperature = temperatureValues.get(i);

                    if (uv != null) {
                        uvValues.add(uv);
                    }

                    if (temperature != null) {
                        temperatures.add(temperature);
                    }

                    break;
                }
            }
        }

        if (uvValues.isEmpty()) {
            return new UvSummary(
                    0.0,
                    0.0,
                    0.0
            );
        }

        double maxUv = uvValues.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        double averageUv = uvValues.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double averageTemperature = temperatures.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return new UvSummary(
                maxUv,
                averageUv,
                averageTemperature
        );
    }


    public RiskLevel calculateRiskLevel(double maxUv) {

        if (maxUv >= 8) {
            return RiskLevel.DANGER;
        }

        if (maxUv >= 5) {
            return RiskLevel.CAUTION;
        }

        return RiskLevel.SAFE;
    }


    public int calculateRequiredSpf(double maxUv) {
        return (int) Math.ceil(maxUv * 3);
    }


    public ExposureResult calculateExposure(
            List<SunlightWindowService.SunlightWindow> windows,
            WeatherResponse weather,
            double seoulDailyAverageExposureScore
    ) {

        if (windows.isEmpty()) {
            return new ExposureResult(
                    0.0,
                    0.0,
                    0,
                    null,
                    null,
                    0,
                    0,
                    RiskLevel.SAFE,
                    0.0,
                    0.0
            );
        }

        UvSummary uvSummary =
                calculateUvSummary(windows, weather);

        LocalDateTime sunlightStart =
                windows.get(0).start();

        LocalDateTime sunlightEnd =
                windows.get(windows.size() - 1).end();

        int sunlightMinutes = (int) windows.stream()
                .mapToLong(
                        SunlightWindowService.SunlightWindow::minutes
                )
                .sum();

        int requiredSpf =
                calculateRequiredSpf(uvSummary.maxUv());

        RiskLevel riskLevel =
                calculateRiskLevel(uvSummary.maxUv());

        int temperature =
                (int) Math.round(
                        uvSummary.averageTemperature()
                );

        double estimatedExposureScore =
                calculateExposureScore(
                        uvSummary.averageUv(),
                        sunlightMinutes
                );

        double koreaComparison =
                calculateKoreaComparison(
                        estimatedExposureScore,
                        seoulDailyAverageExposureScore
                );

        return new ExposureResult(
                uvSummary.maxUv(),
                uvSummary.averageUv(),
                temperature,
                sunlightStart,
                sunlightEnd,
                sunlightMinutes,
                requiredSpf,
                riskLevel,
                estimatedExposureScore,
                koreaComparison
        );
    }


    public record UvSummary(
            double maxUv,
            double averageUv,
            double averageTemperature
    ) {
    }


    public record ExposureResult(
            double maxUv,
            double averageUv,
            int temperature,
            LocalDateTime sunlightStart,
            LocalDateTime sunlightEnd,
            int sunlightMinutes,
            int requiredSpf,
            RiskLevel riskLevel,
            double estimatedExposureScore,
            double koreaComparison
    ) {
    }

    public double calculateExposureScore(
            double averageUv,
            int sunlightMinutes
    ) {
        double sunlightHours = sunlightMinutes / 60.0;

        return sunlightHours * averageUv;
    }

    public double calculateKoreaComparison(
            double exposureScore,
            double seoulDailyAverageExposureScore
    ) {
        if (seoulDailyAverageExposureScore <= 0) {
            return 0.0;
        }

        return exposureScore / seoulDailyAverageExposureScore;
    }
    private String convertWeatherCode(int weatherCode) {

        if (weatherCode == 0) {
            return "맑음";
        }

        if (weatherCode >= 1 && weatherCode <= 3) {
            return "흐림";
        }

        if (weatherCode >= 45 && weatherCode <= 48) {
            return "안개";
        }

        if (weatherCode >= 51 && weatherCode <= 67) {
            return "비";
        }

        if (weatherCode >= 71 && weatherCode <= 77) {
            return "눈";
        }

        if (weatherCode >= 80 && weatherCode <= 82) {
            return "비";
        }

        if (weatherCode >= 85 && weatherCode <= 86) {
            return "눈";
        }

        if (weatherCode >= 95) {
            return "뇌우";
        }

        return "기타";
    }
    public String calculateWeatherCondition(
            List<SunlightWindowService.SunlightWindow> windows,
            WeatherResponse weather
    ) {

        List<String> times = weather.getHourly().getTime();
        List<Integer> weatherCodes = weather.getHourly().getWeatherCode();

        List<Integer> includedCodes = new ArrayList<>();

        for (int i = 0; i < times.size(); i++) {

            LocalDateTime time =
                    LocalDateTime.parse(times.get(i));

            for (SunlightWindowService.SunlightWindow window : windows) {

                boolean included =
                        overlapsHourlyInterval(
                                time,
                                window
                        );

                if (included) {
                    Integer weatherCode = weatherCodes.get(i);

                    if (weatherCode != null) {
                        includedCodes.add(weatherCode);
                    }

                    break;
                }
            }
        }

        if (includedCodes.isEmpty()) {
            return "알 수 없음";
        }

        int representativeCode = includedCodes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        code -> code,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet()
                .stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(0);

        return convertWeatherCode(representativeCode);
    }
    private boolean overlapsHourlyInterval(
            LocalDateTime hourlyStart,
            SunlightWindowService.SunlightWindow window
    ) {
        LocalDateTime hourlyEnd = hourlyStart.plusHours(1);

        return hourlyStart.isBefore(window.end())
                && hourlyEnd.isAfter(window.start());
    }
    public double calculateDailyExposureScore(
            WeatherResponse weather
    ) {

        List<Double> uvIndexes =
                weather.getHourly().getUvIndex();

        if (uvIndexes == null || uvIndexes.isEmpty()) {
            return 0.0;
        }

        double totalUv =
                uvIndexes.stream()
                        .filter(java.util.Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum();

        return totalUv;
    }
    public double calculateSeoulDailyExposureScore(
            WeatherResponse weather
    ) {

        if (weather == null
                || weather.getHourly() == null
                || weather.getDaily() == null
                || weather.getDaily().getSunrise() == null
                || weather.getDaily().getSunset() == null
                || weather.getDaily().getSunrise().isEmpty()
                || weather.getDaily().getSunset().isEmpty()) {

            return 0.0;
        }

        LocalDateTime sunrise =
                LocalDateTime.parse(
                        weather.getDaily()
                                .getSunrise()
                                .get(0)
                );

        LocalDateTime sunset =
                LocalDateTime.parse(
                        weather.getDaily()
                                .getSunset()
                                .get(0)
                );

        List<String> times =
                weather.getHourly().getTime();

        List<Double> uvIndexes =
                weather.getHourly().getUvIndex();

        List<Double> daylightUv =
                new ArrayList<>();

        for (int i = 0; i < times.size(); i++) {

            LocalDateTime time =
                    LocalDateTime.parse(
                            times.get(i)
                    );

            if (time.isBefore(sunrise)
                    || !time.isBefore(sunset)) {
                continue;
            }

            Double uv = uvIndexes.get(i);

            if (uv != null) {
                daylightUv.add(uv);
            }
        }

        if (daylightUv.isEmpty()) {
            return 0.0;
        }

        double averageUv =
                daylightUv.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        int sunlightMinutes =
                (int) java.time.Duration
                        .between(
                                sunrise,
                                sunset
                        )
                        .toMinutes();

        return calculateExposureScore(
                averageUv,
                sunlightMinutes
        );

    }
    public double calculateSeoulComparableExposureScore(
            WeatherResponse seoulWeather,
            int targetSunlightMinutes
    ) {

        if (seoulWeather == null
                || seoulWeather.getHourly() == null
                || seoulWeather.getHourly().getUvIndex() == null) {
            return 0.0;
        }

        List<Double> uvIndexes =
                seoulWeather.getHourly().getUvIndex();

        double averageUv =
                uvIndexes.stream()
                        .filter(java.util.Objects::nonNull)
                        .filter(uv -> uv > 0)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        return calculateExposureScore(
                averageUv,
                targetSunlightMinutes
        );
    }

}