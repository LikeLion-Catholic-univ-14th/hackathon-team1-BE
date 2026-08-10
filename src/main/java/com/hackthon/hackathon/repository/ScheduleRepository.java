package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}
