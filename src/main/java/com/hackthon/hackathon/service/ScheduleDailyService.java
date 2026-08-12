package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.ScheduleDailyResponse;
import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleDailyService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final WeatherService weatherService;
    private final ExposureCalculationService exposureCalculationService;

    public ScheduleDailyResponse getDaily(
            Long userId,
            LocalDate date
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        LocalDateTime endOfDay =
                date.atTime(LocalTime.MAX);

        Schedule schedule =
                scheduleRepository
                        .findFirstByUserAndArrivalTimeLessThanEqualOrderByArrivalTimeDesc(
                                user,
                                endOfDay
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "해당 날짜의 체류 일정이 없습니다."
                                )
                        );

        return createResponse(
                schedule,
                date
        );
    }

    // 토글 API용
    public ScheduleDailyResponse getDailyBySchedule(
            Schedule schedule
    ) {

        LocalDate date =
                schedule.getArrivalTime()
                        .toLocalDate();

        return createResponse(
                schedule,
                date
        );
    }

    // 캘린더용
    public ScheduleDailyResponse getDailyBySchedule(
            Schedule schedule,
            LocalDate date
    ) {

        return createResponse(
                schedule,
                date
        );
    }

    private ScheduleDailyResponse createResponse(
            Schedule schedule,
            LocalDate date
    ) {

        String flightStatus =
                calculateFlightStatus(
                        schedule
                );

        if ("IN_FLIGHT".equals(flightStatus)) {

            return new ScheduleDailyResponse(
                    schedule.getId(),
                    date.toString(),
                    schedule.getDepartureAirport()
                            + " → "
                            + schedule.getArrivalAirport(),
                    exposureCalculationService
                            .calculateRiskLevel(0),
                    flightStatus,
                    schedule.isOuting(),
                    null
            );
        }

        String airportCode =
                schedule.getArrivalAirport();

        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );

        List<ScheduleDailyResponse.UvPoint> graph =
                createUvGraph(
                        weather,
                        date
                );

        double maxUv =
                graph.stream()
                        .mapToDouble(
                                ScheduleDailyResponse.UvPoint::uvValue
                        )
                        .max()
                        .orElse(0.0);

        String warningMessage =
                createWarningMessage(
                        maxUv
                );

        return new ScheduleDailyResponse(
                schedule.getId(),
                date.toString(),
                schedule.getDepartureAirport()
                        + " → "
                        + schedule.getArrivalAirport(),
                exposureCalculationService
                        .calculateRiskLevel(
                                maxUv
                        ),
                flightStatus,
                schedule.isOuting(),
                new ScheduleDailyResponse.UvDetail(
                        graph,
                        warningMessage
                )
        );
    }

    private String calculateFlightStatus(
            Schedule schedule
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        if (!now.isBefore(
                schedule.getDepartureTime()
        ) && now.isBefore(
                schedule.getArrivalTime()
        )) {

            return "IN_FLIGHT";
        }

        if (now.isBefore(
                schedule.getDepartureTime()
        )) {

            return "SCHEDULED";
        }

        return "ARRIVED";
    }

    private List<ScheduleDailyResponse.UvPoint> createUvGraph(
            WeatherResponse weather,
            LocalDate date
    ) {

        if (weather == null
                || weather.getHourly() == null
                || weather.getHourly().getTime() == null
                || weather.getHourly().getUvIndex() == null) {

            return List.of();
        }

        List<String> times =
                weather.getHourly()
                        .getTime();

        List<Double> uvIndexes =
                weather.getHourly()
                        .getUvIndex();

        int size =
                Math.min(
                        times.size(),
                        uvIndexes.size()
                );

        return java.util.stream.IntStream
                .range(0, size)
                .mapToObj(i -> {

                    LocalDateTime time =
                            LocalDateTime.parse(
                                    times.get(i)
                            );

                    Double uv =
                            uvIndexes.get(i);

                    if (!time.toLocalDate().equals(date)
                            || uv == null) {

                        return null;
                    }

                    return new ScheduleDailyResponse.UvPoint(
                            time.toLocalTime()
                                    .toString(),
                            uv
                    );
                })
                .filter(
                        java.util.Objects::nonNull
                )
                .toList();
    }

    private String createWarningMessage(
            double maxUv
    ) {

        if (maxUv >= 8) {
            return "자외선이 매우 강한 날입니다.";
        }

        if (maxUv >= 5) {
            return "자외선 노출에 주의가 필요합니다.";
        }

        return "자외선 위험이 낮은 편입니다.";
    }
}