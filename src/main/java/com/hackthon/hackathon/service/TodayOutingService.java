package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.today.TodayOutingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodayOutingService {

    private final TodayService todayService;
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


        /*
         * 일정 유무와 관계없이 현재 위치의 현지 날짜를 기준으로 저장한다.
         * GET /today, 날짜 상세, 월말 리포트가 모두 DailyOuting을 읽는다.
         */
        dailyOutingService.updateOuting(
                userId,
                todayInfo.localDate(),
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
