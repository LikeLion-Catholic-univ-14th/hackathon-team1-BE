package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SunlightWindowService {

    private final ScheduleRepository scheduleRepository;

    /**
     * 비행/레이오버 일정이 있는 날
     */
    public Optional<AvailableWindow> calculateAvailableWindow(
            LocalDateTime arrivalTime,
            LocalDateTime nextDepartureTime,
            boolean quickTurn
    ) {

        if (quickTurn) {
            return Optional.empty();
        }

        if (arrivalTime == null || nextDepartureTime == null) {
            return Optional.empty();
        }

        // 호텔 도착까지 2시간
        LocalDateTime hotelArrival =
                arrivalTime.plusHours(2);

        // 수면 8시간
        LocalDateTime expectedWakeUp =
                hotelArrival.plusHours(8);

        // 다음 비행 3시간 전부터 준비
        LocalDateTime preparationStart =
                nextDepartureTime.minusHours(3);

        if (!expectedWakeUp.isBefore(preparationStart)) {
            return Optional.empty();
        }

        return Optional.of(
                new AvailableWindow(
                        expectedWakeUp,
                        preparationStart
                )
        );
    }

    /**
     * 비행 일정 없는 소속공항 대기일
     *
     * 일정상 제한이 없으므로 하루 전체를 AvailableWindow로 만들고,
     * 이후 calculateSunlightWindows()에서
     * 실제 일출~일몰과 교집합을 구한다.
     */
    public AvailableWindow calculateBaseDayAvailableWindow(
            LocalDate date
    ) {

        return new AvailableWindow(
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
        );
    }

    public record AvailableWindow(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }

    public Optional<SunlightWindow> calculateSunlightWindow(
            AvailableWindow availableWindow,
            LocalDateTime sunrise,
            LocalDateTime sunset
    ) {

        LocalDateTime start =
                availableWindow.start().isAfter(sunrise)
                        ? availableWindow.start()
                        : sunrise;

        LocalDateTime end =
                availableWindow.end().isBefore(sunset)
                        ? availableWindow.end()
                        : sunset;

        if (!start.isBefore(end)) {
            return Optional.empty();
        }

        long minutes =
                java.time.Duration
                        .between(start, end)
                        .toMinutes();

        return Optional.of(
                new SunlightWindow(
                        start,
                        end,
                        minutes
                )
        );
    }

    public record SunlightWindow(
            LocalDateTime start,
            LocalDateTime end,
            long minutes
    ) {
    }

    public List<SunlightWindow> calculateSunlightWindows(
            AvailableWindow availableWindow,
            WeatherResponse weather
    ) {

        List<SunlightWindow> windows =
                new ArrayList<>();

        if (weather == null
                || weather.getDaily() == null
                || weather.getDaily().getSunrise() == null
                || weather.getDaily().getSunset() == null) {

            return windows;
        }

        List<String> sunrises =
                weather.getDaily().getSunrise();

        List<String> sunsets =
                weather.getDaily().getSunset();

        int size =
                Math.min(
                        sunrises.size(),
                        sunsets.size()
                );

        for (int i = 0; i < size; i++) {

            LocalDateTime sunrise =
                    LocalDateTime.parse(
                            sunrises.get(i)
                    );

            LocalDateTime sunset =
                    LocalDateTime.parse(
                            sunsets.get(i)
                    );

            Optional<SunlightWindow> window =
                    calculateSunlightWindow(
                            availableWindow,
                            sunrise,
                            sunset
                    );

            window.ifPresent(
                    windows::add
            );
        }

        return windows;
    }

    public long calculateTotalSunlightMinutes(
            List<SunlightWindow> windows
    ) {

        return windows.stream()
                .mapToLong(
                        SunlightWindow::minutes
                )
                .sum();
    }
}