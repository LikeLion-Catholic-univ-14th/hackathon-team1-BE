package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.repository.DailyOutingRepository;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodayService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final DailyOutingRepository dailyOutingRepository;

    public TodayScheduleInfo getTodayScheduleInfo(
            Long userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 유저를 찾을 수 없습니다."
                                )
                        );

        LocalDate today =
                LocalDate.now();

        LocalDateTime now =
                LocalDateTime.now();

        List<Schedule> schedules =
                scheduleRepository
                        .findByUserOrderByDepartureTimeAsc(
                                user
                        );

        Schedule schedule =
                findScheduleForDate(
                        schedules,
                        today
                );

        /*
         * ==========================================
         * 일정 없는 날
         * ==========================================
         */
        if (schedule == null) {

            String baseAirportCode =
                    convertBaseAirportToAirportCode(
                            user.getBaseAirport()
                    );

            boolean outing =
                    dailyOutingRepository
                            .findByUserAndDate(
                                    user,
                                    today
                            )
                            .map(DailyOuting::isOuting)
                            .orElse(false);

            return new TodayScheduleInfo(
                    baseAirportCode,
                    null,
                    null,
                    false,
                    outing,
                    null,
                    true
            );
        }


        /*
         * ==========================================
         * 일정 있는 날
         * ==========================================
         */

        String airportCode;

        /*
         * 출발 전 또는 비행 중
         *
         * 아직 도착 공항에 도착하지 않았으므로
         * 출발 공항 기준으로 홈 위치 표시
         */
        if (now.isBefore(
                schedule.getArrivalTime()
        )) {

            airportCode =
                    schedule.getDepartureAirport();

        } else {

            /*
             * 도착 완료
             * → 도착 공항 기준
             */
            airportCode =
                    schedule.getArrivalAirport();
        }


        /*
         * 해당 현재 위치에서 출발하는 다음 일정 찾기
         */
        Schedule nextSchedule =
                schedules.stream()
                        .filter(next ->
                                next.getDepartureAirport()
                                        .equals(
                                                airportCode
                                        )
                        )
                        .filter(next ->
                                next.getDepartureTime()
                                        .isAfter(now)
                        )
                        .min(
                                Comparator.comparing(
                                        Schedule::getDepartureTime
                                )
                        )
                        .orElse(null);


        LocalDateTime nextDepartureTime =
                nextSchedule != null
                        ? nextSchedule.getDepartureTime()
                        : null;


        /*
         * 햇빛창 계산에 사용할 기준 도착시간
         *
         * 현재 schedule이 아직 출발 전/비행 중이라면
         * 현재 schedule의 arrivalTime을 넣으면
         * 출발지 체류시간 계산이 꼬일 수 있음.
         *
         * 그래서 현재 위치에 가장 최근 도착한 일정을 찾는다.
         */
        LocalDateTime stayArrivalTime =
                findLatestArrivalTimeAtAirport(
                        schedules,
                        airportCode,
                        now
                );

        /*
         * 최근 도착 일정이 없으면
         * 현재 schedule의 arrivalTime 대신
         * 오늘 시작 시각을 임시 기준으로 사용
         *
         * 단, 실제 레이오버라면 보통 최근 도착 일정이 존재함.
         */
        if (stayArrivalTime == null) {

            stayArrivalTime =
                    today.atStartOfDay();
        }


        return new TodayScheduleInfo(
                airportCode,
                stayArrivalTime,
                nextDepartureTime,
                schedule.isQuickTurn(),
                schedule.isOuting(),
                schedule,
                false
        );
    }


    /**
     * 오늘 날짜에 실제 비행 또는 레이오버가 있는지 탐색
     */
    private Schedule findScheduleForDate(
            List<Schedule> schedules,
            LocalDate date
    ) {

        /*
         * 1. 실제 비행일
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
         * 2. 레이오버
         *
         * 도착 후 같은 공항에서 출발하는
         * 다음 비행 전까지
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

            if (nextDeparture == null) {
                continue;
            }

            LocalDate nextDepartureDate =
                    nextDeparture
                            .getDepartureTime()
                            .toLocalDate();

            if (!date.isBefore(arrivalDate)
                    && date.isBefore(nextDepartureDate)) {

                return current;
            }
        }

        return null;
    }


    /**
     * 현재 공항에 가장 최근에 도착한 시간 찾기
     */
    private LocalDateTime findLatestArrivalTimeAtAirport(
            List<Schedule> schedules,
            String airportCode,
            LocalDateTime now
    ) {

        return schedules.stream()
                .filter(schedule ->
                        schedule.getArrivalAirport()
                                .equals(
                                        airportCode
                                )
                )
                .filter(schedule ->
                        !schedule.getArrivalTime()
                                .isAfter(now)
                )
                .map(
                        Schedule::getArrivalTime
                )
                .max(
                        LocalDateTime::compareTo
                )
                .orElse(null);
    }


    private String convertBaseAirportToAirportCode(
            BaseAirport baseAirport
    ) {

        if (baseAirport == null) {
            throw new IllegalStateException(
                    "사용자의 소속 공항이 등록되어 있지 않습니다."
            );
        }

        return switch (baseAirport) {
            case INCHEON -> "ICN";
            case GIMPO -> "GMP";
        };
    }


    public record TodayScheduleInfo(
            String airportCode,
            LocalDateTime arrivalTime,
            LocalDateTime nextDepartureTime,
            boolean quickTurn,
            boolean outing,

            // null이면 일정 없는 소속공항 대기일
            Schedule schedule,

            boolean baseDay
    ) {
    }


    public Schedule getCurrentSchedule(
            Long userId
    ) {

        TodayScheduleInfo info =
                getTodayScheduleInfo(
                        userId
                );

        return info.schedule();
    }
}