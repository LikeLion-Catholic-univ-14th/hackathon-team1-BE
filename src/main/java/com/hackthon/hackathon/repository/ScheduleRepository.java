package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    // 특정 사용자의 전체 일정 조회
    List<Schedule> findByUserOrderByDepartureTimeAsc(User user);

    // 특정 사용자의 기간 내 일정 조회
    List<Schedule> findByUserAndDepartureTimeBetweenOrderByDepartureTimeAsc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );
    Optional<Schedule>
    findFirstByUserAndArrivalTimeBeforeOrderByArrivalTimeDesc(
            User user,
            LocalDateTime now
    );

    // 현재 도착지에서 출발하는 가장 가까운 다음 비행편 조회
    // 예: ICN → SYD 도착 후, SYD에서 출발하는 가장 빠른 일정
    Optional<Schedule>
    findFirstByUserAndDepartureAirportAndDepartureTimeAfterOrderByDepartureTimeAsc(
            User user,
            String departureAirport,
            LocalDateTime after
    );
    Optional<Schedule>
    findFirstByUserAndArrivalTimeLessThanEqualOrderByArrivalTimeDesc(
            User user,
            LocalDateTime dateTime
    );

}
