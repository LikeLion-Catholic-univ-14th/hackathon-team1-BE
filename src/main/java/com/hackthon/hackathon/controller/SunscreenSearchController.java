package com.hackthon.hackathon.controller;

// 👇 수정됨: DTO가 있는 정확한 경로(today 추가)로 임포트 해야 합니다!
import com.hackthon.hackathon.dto.today.SunscreenSearchResponse;

import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.repository.SunscreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sunscreens")
public class SunscreenSearchController {
    private final SunscreenRepository sunscreenRepository;

    @GetMapping("/search")
    public List<SunscreenSearchResponse> findBySunnameContaining(@RequestParam String keyword) {

        List<Sunscreen> sunscreens = sunscreenRepository.findByNameContainingOrBrandContaining(keyword, keyword);

        return sunscreens.stream()
                .map(SunscreenSearchResponse::fromEntity)
                .collect(Collectors.toList());
    }
}