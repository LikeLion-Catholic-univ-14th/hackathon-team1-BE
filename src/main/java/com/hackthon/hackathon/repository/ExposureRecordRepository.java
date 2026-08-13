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

    Optional<ExposureRecord>
    findByScheduleAndDateAndLocationType(
            Schedule schedule,
            LocalDate date,
            LocationType locationType
    );

    List<ExposureRecord>
    findBySchedule(
            Schedule schedule
    );

    void deleteBySchedule(Schedule schedule);
}