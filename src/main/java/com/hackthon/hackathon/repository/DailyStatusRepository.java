package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.entity.DailyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyStatusRepository
        extends JpaRepository<DailyStatus, Long> {

    Optional<DailyStatus> findByUserAndDate(
            User user,
            LocalDate date
    );
}