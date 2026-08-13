package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
}
