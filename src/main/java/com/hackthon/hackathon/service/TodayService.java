package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.repository.DailyOutingRepository;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import com.hackthon.hackathon.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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


        String baseAirportCode =
                convertBaseAirportToAirportCode(
                        user.getBaseAirport()
                );


        /*
         * DB의 Schedule 시간은 UTC LocalDateTime이므로
         * 현재시각도 명시적으로 UTC로 맞춘다.
         */
        LocalDateTime nowUtc =
                LocalDateTime.ofInstant(
                        Instant.now(),
                        ZoneOffset.UTC
                );


        List<Schedule> schedules =
                scheduleRepository
                        .findByUserOrderByDepartureTimeAsc(
                                user
                        );


        CurrentScheduleContext context =
                findCurrentSchedule(
                        schedules,
                        nowUtc,
                        baseAirportCode
                );


        // ==========================================
        // 일정 없는 소속공항 대기일
        // ==========================================

        if (context == null) {

            LocalDate localDate =
                    TimeZoneUtil.fromUtc(
                                    nowUtc,
                                    baseAirportCode
                            )
                            .toLocalDate();


            boolean outing =
                    dailyOutingRepository
                            .findByUserAndDate(
                                    user,
                                    localDate
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
                    true,
                    localDate
            );
        }


        // ==========================================
        // 일정 있는 날
        // ==========================================

        Schedule schedule =
                context.schedule();

        String airportCode =
                context.airportCode();


        /*
         * 현재 위치의 현지 날짜
         */
        LocalDate localDate =
                TimeZoneUtil.fromUtc(
                                nowUtc,
                                airportCode
                        )
                        .toLocalDate();


        /*
         * 현재 위치에서 앞으로 출발하는
         * 가장 가까운 일정
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
                                        .isAfter(
                                                nowUtc
                                        )
                        )
                        .min(
                                Comparator.comparing(
                                        Schedule::getDepartureTime
                                )
                        )
                        .orElse(null);


        /*
         * DB UTC → 현재 공항 현지시간
         *
         * SunlightWindowService는 날씨 API의
         * 현지시간과 비교하므로 여기서 현지시간으로 바꾼다.
         */
        LocalDateTime nextDepartureTime =
                nextSchedule != null
                        ? TimeZoneUtil.fromUtc(
                        nextSchedule.getDepartureTime(),
                        airportCode
                )
                        : null;


        /*
         * 현재 공항에 가장 최근 도착한 UTC 시간
         */
        LocalDateTime stayArrivalUtc =
                findLatestArrivalTimeAtAirport(
                        schedules,
                        airportCode,
                        nowUtc
                );


        LocalDateTime stayArrivalTime;

        if (stayArrivalUtc != null) {

            /*
             * UTC → 현재 위치 현지시간
             */
            stayArrivalTime =
                    TimeZoneUtil.fromUtc(
                            stayArrivalUtc,
                            airportCode
                    );

        } else {

            /*
             * 아직 해당 공항으로 도착한 기록이 없다면
             * 현재 위치 현지 날짜의 00:00 사용
             */
            stayArrivalTime =
                    localDate.atStartOfDay();
        }


        return new TodayScheduleInfo(
                airportCode,
                stayArrivalTime,
                nextDepartureTime,
                schedule.isQuickTurn(),
                schedule.isOuting(),
                schedule,
                false,
                localDate
        );
    }


    // ==========================================
    // 현재 일정 / 현재 위치 찾기
    // ==========================================

    private CurrentScheduleContext findCurrentSchedule(
            List<Schedule> schedules,
            LocalDateTime nowUtc,
            String baseAirportCode
    ) {

        /*
         * ==========================================
         * 1. 현재 실제 비행 중
         * ==========================================
         *
         * departure <= now < arrival
         */
        Schedule activeFlight =
                schedules.stream()
                        .filter(schedule ->
                                !nowUtc.isBefore(
                                        schedule.getDepartureTime()
                                )
                        )
                        .filter(schedule ->
                                nowUtc.isBefore(
                                        schedule.getArrivalTime()
                                )
                        )
                        .min(
                                Comparator.comparing(
                                        Schedule::getDepartureTime
                                )
                        )
                        .orElse(null);


        if (activeFlight != null) {

            /*
             * 기존 홈 정책 유지:
             * 도착 전까지는 출발 공항 기준
             */
            return new CurrentScheduleContext(
                    activeFlight,
                    activeFlight.getDepartureAirport()
            );
        }


        /*
         * ==========================================
         * 2. 해외 레이오버
         * ==========================================
         *
         * 이미 도착했고,
         * 해당 도착 공항에서 출발하는
         * 다음 일정이 아직 남아 있는 경우.
         */
        Schedule layoverSchedule =
                schedules.stream()
                        .filter(schedule ->
                                !schedule.getArrivalTime()
                                        .isAfter(
                                                nowUtc
                                        )
                        )
                        .filter(schedule ->
                                !schedule.getArrivalAirport()
                                        .equals(
                                                baseAirportCode
                                        )
                        )
                        .filter(schedule ->
                                hasFutureDepartureFromAirport(
                                        schedules,
                                        schedule.getArrivalAirport(),
                                        schedule.getArrivalTime(),
                                        nowUtc
                                )
                        )
                        .max(
                                Comparator.comparing(
                                        Schedule::getArrivalTime
                                )
                        )
                        .orElse(null);


        if (layoverSchedule != null) {

            return new CurrentScheduleContext(
                    layoverSchedule,
                    layoverSchedule.getArrivalAirport()
            );
        }


        /*
         * ==========================================
         * 3. 오늘 소속공항에서 출발 예정
         * ==========================================
         *
         * 아직 출발하지 않았지만
         * 현지 기준 오늘 출발하는 일정이 있으면
         * 홈에 해당 일정 표시.
         */
        LocalDate baseLocalToday =
                TimeZoneUtil.fromUtc(
                                nowUtc,
                                baseAirportCode
                        )
                        .toLocalDate();


        Schedule upcomingToday =
                schedules.stream()
                        .filter(schedule ->
                                schedule.getDepartureAirport()
                                        .equals(
                                                baseAirportCode
                                        )
                        )
                        .filter(schedule ->
                                schedule.getDepartureTime()
                                        .isAfter(
                                                nowUtc
                                        )
                        )
                        .filter(schedule -> {

                            LocalDate localDepartureDate =
                                    TimeZoneUtil.fromUtc(
                                                    schedule.getDepartureTime(),
                                                    schedule.getDepartureAirport()
                                            )
                                            .toLocalDate();

                            return localDepartureDate
                                    .equals(
                                            baseLocalToday
                                    );
                        })
                        .min(
                                Comparator.comparing(
                                        Schedule::getDepartureTime
                                )
                        )
                        .orElse(null);


        if (upcomingToday != null) {

            return new CurrentScheduleContext(
                    upcomingToday,
                    upcomingToday.getDepartureAirport()
            );
        }


        /*
         * 비행도 아니고
         * 해외 레이오버도 아니고
         * 오늘 출발 예정도 없음
         */
        return null;
    }


    // ==========================================
    // 해당 공항에서 앞으로 출발 일정이 있는지
    // ==========================================

    private boolean hasFutureDepartureFromAirport(
            List<Schedule> schedules,
            String airportCode,
            LocalDateTime arrivalTimeUtc,
            LocalDateTime nowUtc
    ) {

        return schedules.stream()
                .anyMatch(next ->
                        next.getDepartureAirport()
                                .equals(
                                        airportCode
                                )
                                && next.getDepartureTime()
                                .isAfter(
                                        arrivalTimeUtc
                                )
                                && next.getDepartureTime()
                                .isAfter(
                                        nowUtc
                                )
                );
    }


    // ==========================================
    // 현재 공항에 가장 최근 도착한 UTC 시간
    // ==========================================

    private LocalDateTime findLatestArrivalTimeAtAirport(
            List<Schedule> schedules,
            String airportCode,
            LocalDateTime nowUtc
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
                                .isAfter(
                                        nowUtc
                                )
                )
                .map(
                        Schedule::getArrivalTime
                )
                .max(
                        LocalDateTime::compareTo
                )
                .orElse(null);
    }


    // ==========================================
    // 소속공항 enum → IATA
    // ==========================================

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


    // ==========================================
    // 내부 현재 일정 정보
    // ==========================================

    private record CurrentScheduleContext(
            Schedule schedule,
            String airportCode
    ) {
    }


    // ==========================================
    // Controller 전달 정보
    // ==========================================

    public record TodayScheduleInfo(
            String airportCode,

            /*
             * 현재 위치 현지시간
             */
            LocalDateTime arrivalTime,

            /*
             * 현재 위치 현지시간
             */
            LocalDateTime nextDepartureTime,

            boolean quickTurn,
            boolean outing,

            // null이면 일정 없는 소속공항 대기일
            Schedule schedule,

            boolean baseDay,

            /*
             * 현재 위치 기준 현지 날짜
             */
            LocalDate localDate
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