package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.CalendarResponse;
import com.hackthon.hackathon.dto.ScheduleDailyResponse;
import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.RiskLevel;
import com.hackthon.hackathon.repository.DailyOutingRepository;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleCalendarService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleDailyService scheduleDailyService;
    private final DailyOutingRepository dailyOutingRepository;
    private final ScheduleDateResolverService scheduleDateResolverService;


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

        List<CalendarResponse.DayInfo> days =
                new ArrayList<>();


        for (int day = 1;
             day <= yearMonth.lengthOfMonth();
             day++) {

            LocalDate date =
                    yearMonth.atDay(day);


            Schedule schedule =
                    scheduleDateResolverService.findScheduleForDate(
                            schedules,
                            date
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


                boolean isOuting =
                        dailyOutingRepository
                                .findByUserAndDate(
                                        user,
                                        date
                                )
                                .map(DailyOuting::isOuting)
                                .orElse(true);

                if (!isOuting) {

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


}
