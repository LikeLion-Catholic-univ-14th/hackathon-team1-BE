package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.ExposureRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExposureRecordRepository extends JpaRepository<ExposureRecord, Long> {
}