package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.SunscreenSaveRequest;
import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.repository.SunscreenRepository;
import com.hackthon.hackathon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class SunscreenSaveController {
    private final SunscreenRepository sunscreenRepository;
    private final UserRepository userRepository;

    @PostMapping("/{userId}/sunscreen")
    public ResponseEntity<String> saveSunscreens(
            @PathVariable Long userId,
            @RequestBody SunscreenSaveRequest requestDto) {

        // 1. 유저 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        // 2. DTO 리스트를 엔티티 리스트로 변환
        List<Sunscreen> sunscreens = requestDto.getSunscreens().stream()
                .map(info -> Sunscreen.builder()
                        .user(user)
                        .brand(info.getBrand())
                        .name(info.getName())
                        .filterType(info.getFilterType())
                        .productType(info.getProductType())
                        .spf(info.getSpf())
                        .pa(info.getPa())
                        .build())
                .collect(Collectors.toList());

        // 3. 한 번에 저장

        sunscreenRepository.saveAll(sunscreens);
        return ResponseEntity.ok("선크림 저장 성공!");
    }
}