package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.DailyOutingResponse;
import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.DailyOutingRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyOutingService {

    private final UserRepository userRepository;
    private final DailyOutingRepository dailyOutingRepository;

    private final BaseDayExposureService baseDayExposureService;


    @Transactional
    public DailyOutingResponse updateOuting(
            Long userId,
            LocalDate date,
            boolean outing
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 유저를 찾을 수 없습니다."
                                )
                        );


        // ==========================================
        // 1. DailyOuting 생성 / 조회
        // ==========================================

        DailyOuting dailyOuting =
                dailyOutingRepository
                        .findByUserAndDate(
                                user,
                                date
                        )
                        .orElseGet(() ->
                                DailyOuting.builder()
                                        .user(user)
                                        .date(date)
                                        .outing(outing)
                                        .build()
                        );


        // ==========================================
        // 2. 외출 상태 변경
        // ==========================================

        dailyOuting.updateOuting(
                outing
        );


        // ==========================================
        // 3. DailyOuting 저장
        // ==========================================

        dailyOutingRepository.save(
                dailyOuting
        );


        // ==========================================
        // 4. 과거 / 오늘이면 ExposureRecord 반영
        // ==========================================

        if (!date.isAfter(LocalDate.now())) {

            baseDayExposureService
                    .createOrUpdate(
                            user,
                            date,
                            outing
                    );
        }


        // ==========================================
        // 5. 응답
        // ==========================================

        return new DailyOutingResponse(
                date.toString(),
                dailyOuting.isOuting()
        );
    }
}