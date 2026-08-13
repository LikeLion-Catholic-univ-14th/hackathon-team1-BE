package com.hackthon.hackathon.service;
import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SunlightWindowService {
    private final ScheduleRepository scheduleRepository;

    /*public Optional<AvailableWindow> calculateAvailableWindow(
            Schedule schedule
    ) {

        // 퀵턴이면 체류 외출시간 계산하지 않음
        if (schedule.isQuickTurn()) {
            return Optional.empty();
        }

        Optional<Schedule> nextSchedule =
                scheduleRepository
                        .findFirstByUserAndDepartureAirportAndDepartureTimeAfterOrderByDepartureTimeAsc(
                                schedule.getUser(),
                                schedule.getArrivalAirport(),
                                schedule.getArrivalTime()
                        );

        // 다음 출발편이 없으면 계산 불가
        if (nextSchedule.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime hotelArrival =
                schedule.getArrivalTime().plusHours(2);

        LocalDateTime expectedWakeUp =
                hotelArrival.plusHours(8);

        LocalDateTime preparationStart =
                nextSchedule.get()
                        .getDepartureTime()
                        .minusHours(3);

        // 기상 예상보다 준비 시작이 빠르면 외출 가능시간 없음
        if (!expectedWakeUp.isBefore(preparationStart)) {
            return Optional.empty();
        }

        return Optional.of(
                new AvailableWindow(
                        expectedWakeUp,
                        preparationStart
                )
        );
    }*/

    public Optional<AvailableWindow> calculateAvailableWindow(
            LocalDateTime arrivalTime,
            LocalDateTime nextDepartureTime,
            boolean quickTurn
    ) {

        // 퀵턴이면 외출 가능 구간 없음
        if (quickTurn) {
            return Optional.empty();
        }

        // 기획 계산식
        LocalDateTime hotelArrival =
                arrivalTime.plusHours(2);

        LocalDateTime expectedWakeUp =
                hotelArrival.plusHours(8);

        LocalDateTime preparationStart =
                nextDepartureTime.minusHours(3);

        // 자고 일어났더니 이미 다음 비행 준비시간이면 외출 불가능
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

        long minutes = java.time.Duration
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

        List<SunlightWindow> windows = new ArrayList<>();

        List<String> sunrises = weather.getDaily().getSunrise();
        List<String> sunsets = weather.getDaily().getSunset();

        for (int i = 0; i < sunrises.size(); i++) {

            LocalDateTime sunrise =
                    LocalDateTime.parse(sunrises.get(i));

            LocalDateTime sunset =
                    LocalDateTime.parse(sunsets.get(i));

            Optional<SunlightWindow> window =
                    calculateSunlightWindow(
                            availableWindow,
                            sunrise,
                            sunset
                    );

            window.ifPresent(windows::add);
        }

        return windows;
    }
    public long calculateTotalSunlightMinutes(
            List<SunlightWindow> windows
    ) {
        return windows.stream()
                .mapToLong(SunlightWindow::minutes)
                .sum();
    }
}
