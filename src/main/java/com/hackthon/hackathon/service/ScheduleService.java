package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.*;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.SunscreenRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final SunscreenRepository sunscreenRepository;

    // 일정 여러 개 최종 등록
    @Transactional
    public ScheduleCreateResponse createSchedule(
            Long userId,
            ScheduleCreateRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        if (request.schedules() == null
                || request.schedules().isEmpty()) {

            throw new IllegalArgumentException(
                    "등록할 일정이 없습니다."
            );
        }

        List<Schedule> schedules =
                request.schedules()
                        .stream()
                        .map(item ->
                                Schedule.create(
                                        user,
                                        item.flightNumber(),
                                        item.departureAirport(),
                                        item.arrivalAirport(),
                                        item.departureTime(),
                                        item.arrivalTime(),

                                        // 현재 등록 API 명세에는
                                        // quickTurn 필드가 없으므로 기본값
                                        false
                                )
                        )
                        .toList();

        scheduleRepository.saveAll(schedules);

        return new ScheduleCreateResponse(
                "SUCCESS",
                "프로필이 완성되었어요!"
        );
    }


    // 외출 여부 수정
    @Transactional
    public Schedule updateOuting(
            Long scheduleId,
            boolean outing
    ) {

        Schedule schedule =
                scheduleRepository.findById(scheduleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 일정을 찾을 수 없습니다."
                                )
                        );

        schedule.updateOuting(
                outing
        );

        return schedule;
    }



    // 일정 수정
    @Transactional
    public ScheduleUpdateResponse updateSchedule(
            Long scheduleId,
            ScheduleUpdateRequest request
    ) {

        Schedule schedule =
                scheduleRepository.findById(scheduleId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 일정을 찾을 수 없습니다."
                                )
                        );

        schedule.update(
                request.flightNumber(),
                request.departureAirport(),
                request.arrivalAirport(),
                request.departureTime(),
                request.arrivalTime(),
                request.isQuickTurn()
        );

        return new ScheduleUpdateResponse(
                schedule.getId(),
                "비행 일정이 수정되었어요."
        );
    }
    @Transactional
    public void applySolution(
            Long scheduleId,
            SolutionApplyRequest request
    ) {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 일정을 찾을 수 없습니다."
                        )
                );

        Sunscreen sunscreen = sunscreenRepository
                .findById(request.sunscreenId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 선크림을 찾을 수 없습니다."
                        )
                );

        schedule.applySunscreen(
                sunscreen,
                request.isApplied()
        );
    }
}