package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.CalendarResponse;
import com.hackthon.hackathon.dto.ScheduleDailyResponse;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleCalendarService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleDailyService scheduleDailyService;

    public CalendarResponse getCalendar(
            Long userId,
            String month
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        YearMonth yearMonth =
                YearMonth.parse(month);

        List<Schedule> schedules =
                scheduleRepository
                        .findByUserOrderByDepartureTimeAsc(
                                user
                        );

        List<CalendarResponse.DayInfo> days =
                new ArrayList<>();

        for (int day = 1;
             day <= yearMonth.lengthOfMonth();
             day++) {

            LocalDate date =
                    yearMonth.atDay(day);

            Schedule schedule =
                    findScheduleForDate(
                            schedules,
                            date
                    );

            // 실제 일정 / 레이오버가 없는 날
            if (schedule == null) {

                days.add(
                        new CalendarResponse.DayInfo(
                                date.toString(),
                                null,
                                "INDOOR"
                        )
                );

                continue;
            }

            try {

                ScheduleDailyResponse daily =
                        scheduleDailyService
                                .getDailyBySchedule(
                                        schedule,
                                        date
                                );

                String status;

                if (!schedule.isOuting()) {
                    status = "INDOOR";
                } else {
                    status =
                            daily.riskLevel()
                                    .name();
                }

                days.add(
                        new CalendarResponse.DayInfo(
                                date.toString(),
                                schedule.getId(),
                                status
                        )
                );

            } catch (Exception e) {

                days.add(
                        new CalendarResponse.DayInfo(
                                date.toString(),
                                schedule.getId(),
                                "INDOOR"
                        )
                );
            }
        }

        return new CalendarResponse(
                month,
                days
        );
    }

    private Schedule findScheduleForDate(
            List<Schedule> schedules,
            LocalDate date
    ) {

        /*
         * 1순위:
         * 실제 비행이 해당 날짜에 걸쳐 있는 경우.
         *
         * 예:
         * 8/15 출발 → 8/16 도착이면
         * 8/15, 8/16 모두 해당 비행 일정.
         */
        Schedule flightSchedule =
                schedules.stream()
                        .filter(schedule -> {

                            LocalDate departureDate =
                                    schedule.getDepartureTime()
                                            .toLocalDate();

                            LocalDate arrivalDate =
                                    schedule.getArrivalTime()
                                            .toLocalDate();

                            return !date.isBefore(departureDate)
                                    && !date.isAfter(arrivalDate);
                        })
                        .min(
                                Comparator.comparing(
                                        Schedule::getDepartureTime
                                )
                        )
                        .orElse(null);

        if (flightSchedule != null) {
            return flightSchedule;
        }

        /*
         * 2순위:
         * 도착 후 다음 출발 전까지의 레이오버.
         */
        for (Schedule current : schedules) {

            LocalDate arrivalDate =
                    current.getArrivalTime()
                            .toLocalDate();

            if (date.isBefore(arrivalDate)) {
                continue;
            }

            Schedule nextDeparture =
                    schedules.stream()
                            .filter(next ->
                                    next.getDepartureTime()
                                            .isAfter(
                                                    current.getArrivalTime()
                                            )
                            )
                            .filter(next ->
                                    next.getDepartureAirport()
                                            .equals(
                                                    current.getArrivalAirport()
                                            )
                            )
                            .min(
                                    Comparator.comparing(
                                            Schedule::getDepartureTime
                                    )
                            )
                            .orElse(null);

            /*
             * 다음 출발 일정이 없다면
             * 무한정 체류 중이라고 판단하지 않음.
             */
            if (nextDeparture == null) {
                continue;
            }

            LocalDate nextDepartureDate =
                    nextDeparture.getDepartureTime()
                            .toLocalDate();

            /*
             * 도착일부터
             * 다음 출발일 직전까지 레이오버.
             *
             * 다음 출발 당일은 위의 "실제 비행"
             * 판정에서 처리함.
             */
            if (!date.isBefore(arrivalDate)
                    && date.isBefore(nextDepartureDate)) {

                return current;
            }
        }

        return null;
    }
}