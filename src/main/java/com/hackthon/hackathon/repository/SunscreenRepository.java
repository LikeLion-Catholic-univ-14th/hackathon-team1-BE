package com.hackthon.hackathon.repository;

import com.hackthon.hackathon.entity.Sunscreen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SunscreenRepository extends JpaRepository<Sunscreen, Long> {
    //선크림 이름 입력하면서 뜨는 목록 구현
    List<Sunscreen> findByNameContaining(String keyword);
    //유저 아이디로 선크림 목록 조회
    List<Sunscreen> findByUserId(Long userId);
}