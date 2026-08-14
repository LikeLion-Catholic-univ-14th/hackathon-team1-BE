package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.ExposureRecord;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExposureRecordRepository
        extends JpaRepository<ExposureRecord, Long> {

    List<ExposureRecord>
    findByUserAndDateBetweenOrderByDateAsc(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    // 비행/레이오버 일정이 있는 날
    Optional<ExposureRecord>
    findByScheduleAndDateAndLocationType(
            Schedule schedule,
            LocalDate date,
            LocationType locationType
    );

    // 일정 없는 소속공항 대기일
    Optional<ExposureRecord>
    findByUserAndDateAndLocationTypeAndScheduleIsNull(
            User user,
            LocalDate date,
            LocationType locationType
    );

    List<ExposureRecord>
    findBySchedule(
            Schedule schedule
    );

    void deleteBySchedule(
            Schedule schedule
    );
}