package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.CalendarResponse;
import com.hackthon.hackathon.dto.ScheduleDailyResponse;
import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.enums.RiskLevel;
import com.hackthon.hackathon.repository.DailyOutingRepository;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import com.hackthon.hackathon.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleCalendarService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleDailyService scheduleDailyService;
    private final DailyOutingRepository dailyOutingRepository;


    public CalendarResponse getCalendar(
            Long userId,
            String month
    ) {

        User user =
                userRepository.findById(userId)
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

        String baseAirportCode =
                convertBaseAirportToAirportCode(
                        user.getBaseAirport()
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
                            date,
                            baseAirportCode
                    );


            // ==========================================
            // 일정 없는 소속공항 대기일
            // ==========================================

            if (schedule == null) {

                boolean isOuting =
                        dailyOutingRepository
                                .findByUserAndDate(
                                        user,
                                        date
                                )
                                .map(DailyOuting::isOuting)
                                .orElse(false);


                String status;

                if (!isOuting) {

                    status = "INDOOR";

                } else {

                    try {

                        ScheduleDailyResponse daily =
                                scheduleDailyService
                                        .getDaily(
                                                userId,
                                                date
                                        );


                        if (daily.departureInfo() != null
                                && daily.departureInfo().riskLevel() != null) {

                            status =
                                    daily.departureInfo()
                                            .riskLevel()
                                            .name();

                        } else {

                            // 아직 UV 데이터가 없는 날짜
                            status = null;
                        }

                    } catch (Exception e) {

                        log.warn(
                                "대기일 UV 조회 실패. date={}",
                                date,
                                e
                        );

                        status = null;
                    }
                }


                days.add(
                        new CalendarResponse.DayInfo(
                                date.toString(),
                                null,
                                status
                        )
                );

                continue;
            }


            // ==========================================
            // 실제 비행 / 해외 레이오버
            // ==========================================

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

                    RiskLevel riskLevel = null;


                    if (daily.arrivalInfo() != null
                            && daily.arrivalInfo().riskLevel() != null) {

                        riskLevel =
                                daily.arrivalInfo()
                                        .riskLevel();

                    } else if (daily.departureInfo() != null
                            && daily.departureInfo().riskLevel() != null) {

                        riskLevel =
                                daily.departureInfo()
                                        .riskLevel();
                    }


                    /*
                     * UV 데이터 없는 미래 날짜 등은
                     * SAFE로 오판하지 않고 null
                     */
                    status =
                            riskLevel != null
                                    ? riskLevel.name()
                                    : null;
                }


                days.add(
                        new CalendarResponse.DayInfo(
                                date.toString(),
                                schedule.getId(),
                                status
                        )
                );


            } catch (Exception e) {

                log.warn(
                        "캘린더 날짜 조회 실패. date={}, scheduleId={}",
                        date,
                        schedule.getId(),
                        e
                );


                days.add(
                        new CalendarResponse.DayInfo(
                                date.toString(),
                                schedule.getId(),
                                null
                        )
                );
            }
        }


        return new CalendarResponse(
                month,

                // 최초 일정 등록 여부
                user.isHasScheduleHistory(),

                days
        );
    }


    // ==========================================
    // 날짜 기준 일정 / 해외 레이오버 판정
    // ==========================================

    private Schedule findScheduleForDate(
            List<Schedule> schedules,
            LocalDate date,
            String baseAirportCode
    ) {

        /*
         * 1. 실제 비행일
         *
         * departureDate <= date <= arrivalDate
         */
        Schedule flightSchedule =
                schedules.stream()
                        .filter(schedule -> {

                            LocalDate departureDate =
                                    TimeZoneUtil.fromUtc(
                                            schedule.getDepartureTime(),
                                            schedule.getDepartureAirport()
                                    ).toLocalDate();

                            LocalDate arrivalDate =
                                    TimeZoneUtil.fromUtc(
                                            schedule.getArrivalTime(),
                                            schedule.getArrivalAirport()
                                    ).toLocalDate();

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
         * 2. 해외 레이오버
         *
         * 도착 후 같은 공항에서 출발하는
         * 다음 비행 직전까지.
         *
         * 단, 소속공항 도착 후는 레이오버가 아니라 대기일.
         */
        for (Schedule current : schedules) {

            LocalDate arrivalDate =
                    TimeZoneUtil.fromUtc(
                            current.getArrivalTime(),
                            current.getArrivalAirport()
                    ).toLocalDate();


            if (date.isBefore(arrivalDate)) {
                continue;
            }


            /*
             * 귀국 후 소속공항 대기일
             *
             * 예:
             * SYD → ICN 8/15 도착
             * 8/16 일정 없음
             *
             * → 8/16은 이전 schedule에 포함하지 않음.
             */
            if (isKoreanBaseAirport(
                    current.getArrivalAirport()
            )) {

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
                    TimeZoneUtil.fromUtc(
                            nextDeparture.getDepartureTime(),
                            nextDeparture.getDepartureAirport()
                    ).toLocalDate();


            if (!date.isBefore(arrivalDate)
                    && date.isBefore(nextDepartureDate)) {

                return current;
            }
        }


        // 실제 비행도 해외 레이오버도 없음
        return null;
    }


    // ==========================================
    // 소속공항 → IATA
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

    private boolean isKoreanBaseAirport(
            String airportCode
    ) {
        return "ICN".equalsIgnoreCase(airportCode.trim())
                || "GMP".equalsIgnoreCase(airportCode.trim());
    }
}
