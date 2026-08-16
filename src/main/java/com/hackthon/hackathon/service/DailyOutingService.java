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
    private final ExposureRecordService exposureRecordService;


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


        dailyOuting.updateOuting(
                outing
        );


        dailyOutingRepository.save(
                dailyOuting
        );


        /*
         * 월말 리포트가 읽는 ExposureRecord의
         * isOuting 값도 동일하게 갱신
         */
        exposureRecordService
                .updateOutingByDate(
                        user,
                        date,
                        outing
                );


        return new DailyOutingResponse(
                date.toString(),
                dailyOuting.isOuting()
        );
    }
}