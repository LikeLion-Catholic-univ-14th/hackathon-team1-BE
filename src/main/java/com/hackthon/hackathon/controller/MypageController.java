package com.hackthon.hackathon.controller;

import com.hackthon.hackathon.dto.MypageResponse;
import com.hackthon.hackathon.dto.ProfileUpdateRequest;
import com.hackthon.hackathon.service.MypageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/pouch")
    public ResponseEntity<List<MypageResponse.PouchItemDto>> getPouchList() {
        MypageResponse response = mypageService.getMypageProfile();
        return ResponseEntity.ok(response.getPouch());
    }

    @PutMapping("/pouch/{productId}")
    public ResponseEntity<String> updateSunscreen(
            @org.springframework.web.bind.annotation.PathVariable Long productId,
            @org.springframework.web.bind.annotation.RequestBody com.hackthon.hackathon.dto.SunscreenUpdateRequest request) {
        mypageService.updateSunscreen(productId, request);
        return ResponseEntity.ok("선크림 수정 성공!");
    }

    @DeleteMapping("/pouch/{productId}")
    public ResponseEntity<String> deleteSunscreen(@org.springframework.web.bind.annotation.PathVariable Long productId) {
        mypageService.deleteSunscreen(productId);
        return ResponseEntity.ok("선크림 삭제 성공!");
    }

    @PostMapping("/procedures")
    public org.springframework.http.ResponseEntity<String> addProcedure(
            @org.springframework.web.bind.annotation.RequestBody com.hackthon.hackathon.dto.ProcedureDto.Request request) {
        mypageService.addProcedure(request);
        return org.springframework.http.ResponseEntity.ok("시술 이력 등록 성공!");
    }

    @GetMapping("/procedures")
    public org.springframework.http.ResponseEntity<java.util.List<com.hackthon.hackathon.dto.ProcedureDto.Response>> getProcedures() {
        return org.springframework.http.ResponseEntity.ok(mypageService.getProcedures());
    }

    @DeleteMapping("/procedures/{procedureId}")
    public org.springframework.http.ResponseEntity<String> deleteProcedure(
            @org.springframework.web.bind.annotation.PathVariable Long procedureId) {
        mypageService.deleteProcedure(procedureId);
        return org.springframework.http.ResponseEntity.ok("시술 이력 삭제 성공!");
    }

}