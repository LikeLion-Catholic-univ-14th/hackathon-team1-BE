package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TodayService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    public TodayScheduleInfo getTodayScheduleInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        // 현재 시점 기준 가장 최근에 도착한 비행
        Schedule currentSchedule =
                scheduleRepository
                        .findFirstByUserAndArrivalTimeBeforeOrderByArrivalTimeDesc(
                                user,
                                now
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "현재 체류 기준 일정이 없습니다."
                                )
                        );

        // 현재 도착 공항에서 출발하는 다음 비행
        Schedule nextSchedule =
                scheduleRepository
                        .findFirstByUserAndDepartureAirportAndDepartureTimeAfterOrderByDepartureTimeAsc(
                                user,
                                currentSchedule.getArrivalAirport(),
                                currentSchedule.getArrivalTime()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "다음 출발 일정이 없습니다."
                                )
                        );

        return new TodayScheduleInfo(
                currentSchedule.getArrivalAirport(),
                currentSchedule.getArrivalTime(),
                nextSchedule.getDepartureTime(),
                currentSchedule.isQuickTurn(),
                currentSchedule.isOuting()
        );
    }

    public record TodayScheduleInfo(
            String airportCode,
            LocalDateTime arrivalTime,
            LocalDateTime nextDepartureTime,
            boolean quickTurn,
            boolean outing
    ) {
    }
    public Schedule getCurrentSchedule(
            Long userId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 유저를 찾을 수 없습니다."
                        )
                );

        LocalDateTime now =
                LocalDateTime.now();

        return scheduleRepository
                .findFirstByUserAndArrivalTimeBeforeOrderByArrivalTimeDesc(
                        user,
                        now
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "현재 체류 기준 일정이 없습니다."
                        )
                );
    }
}