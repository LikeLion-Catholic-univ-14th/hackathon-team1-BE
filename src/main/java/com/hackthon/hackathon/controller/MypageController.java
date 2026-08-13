package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.MypageResponse;
import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MypageController {
    private final MypageService mypageService;

    @GetMapping("/profile")
    public ResponseEntity<MypageResponse> getMypage() {
        Long demoUserId = 1L;
        MypageResponse response= mypageService.getMypageProfile();
        return ResponseEntity.ok(response);
    }
}
