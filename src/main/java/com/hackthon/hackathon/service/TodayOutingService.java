package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.today.TodayOutingResponse;
import com.hackthon.hackathon.entity.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TodayOutingService {

    private final TodayService todayService;
    private final ScheduleService scheduleService;
    private final DailyOutingService dailyOutingService;

    @Transactional
    public TodayOutingResponse updateOuting(
            Long userId,
            boolean outing
    ) {

        TodayService.TodayScheduleInfo todayInfo =
                todayService.getTodayScheduleInfo(
                        userId
                );


        // ==========================================
        // 일정 없는 소속공항 대기일
        // ==========================================

        if (todayInfo.baseDay()) {

            dailyOutingService.updateOuting(
                    userId,
                    LocalDate.now(),
                    outing
            );

            return new TodayOutingResponse(
                    outing
                            ? "OUTING"
                            : "INDOOR",
                    outing
            );
        }


        // ==========================================
        // 일정 있는 날
        // ==========================================

        Schedule schedule =
                todayInfo.schedule();

        if (schedule == null) {

            throw new IllegalStateException(
                    "오늘 일정 정보를 찾을 수 없습니다."
            );
        }


        scheduleService.updateOuting(
                schedule.getId(),
                outing
        );


        return new TodayOutingResponse(
                outing
                        ? "OUTING"
                        : "INDOOR",
                outing
        );
    }
}