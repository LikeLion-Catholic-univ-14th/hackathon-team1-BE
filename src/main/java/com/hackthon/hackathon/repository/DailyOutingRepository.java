package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyOutingRepository
        extends JpaRepository<DailyOuting, Long> {

    Optional<DailyOuting> findByUserAndDate(
            User user,
            LocalDate date
    );
}