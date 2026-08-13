package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.ProcedureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcedureHistoryRepository extends JpaRepository<ProcedureHistory, Long> {
    List<ProcedureHistory> findByUserId(Long userId);
}