package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.MypageResponse;
import com.hackthon.hackathon.dto.ProfileUpdateRequest;
import com.hackthon.hackathon.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MypageController {
    private final MypageService mypageService;

    @GetMapping("/profile")
    public ResponseEntity<MypageResponse> getMypage() {
        MypageResponse response = mypageService.getMypageProfile();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody ProfileUpdateRequest request) {
        mypageService.updateMypageProfile(request);
        return ResponseEntity.ok("내 정보 수정 성공!");
    }
}