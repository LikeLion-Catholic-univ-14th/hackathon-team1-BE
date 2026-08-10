package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
}