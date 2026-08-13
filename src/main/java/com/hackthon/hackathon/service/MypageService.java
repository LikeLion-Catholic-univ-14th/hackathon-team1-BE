package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.MypageResponse;
import com.hackthon.hackathon.dto.ProcedureDto;
import com.hackthon.hackathon.dto.ProfileUpdateRequest;
import com.hackthon.hackathon.entity.ProcedureHistory;
import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.enums.SkinConcern;
import com.hackthon.hackathon.enums.SkinType;
import com.hackthon.hackathon.repository.SunscreenRepository;
import com.hackthon.hackathon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MypageService {
    private final SunscreenRepository sunscreenRepository;
    private final UserRepository userRepository;
    private final com.hackthon.hackathon.repository.ProcedureHistoryRepository procedureHistoryRepository;

    public MypageResponse getMypageProfile(){
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        List<Sunscreen> sunscreens = sunscreenRepository.findByUserId(user.getId());
        List<MypageResponse.PouchItemDto> pouchDtos = sunscreens.stream()
                .map(sunscreen -> MypageResponse.PouchItemDto.builder()
                        .productId(sunscreen.getId())
                        .name(sunscreen.getName())
                        .productType(sunscreen.getProductType() != null ? sunscreen.getProductType().name() : null)
                        .filterType(sunscreen.getFilterType() != null ? sunscreen.getFilterType().name() : null)
                        .spf(sunscreen.getSpf())
                        .pa(sunscreen.getPa() != null ? sunscreen.getPa().name() : null)
                        .build())
                .collect(Collectors.toList());

        // 4. 유저의 피부 타입 및 고민 Enum 리스트를 String 리스트로 변환
        List<String> skinTypeStrings = user.getSkinTypes() != null ?
                user.getSkinTypes().stream().map(Enum::name).collect(Collectors.toList()) : null;

        List<String> skinConcernStrings = user.getSkinConcerns() != null ?
                user.getSkinConcerns().stream().map(Enum::name).collect(Collectors.toList()) : null;

        // 5. 최종 응답 DTO 빌드 및 반환
        return MypageResponse.builder()
                .name(user.getName())
                .baseAirport(user.getBaseAirport() != null ? user.getBaseAirport().name() : null)
                .skinTypes(skinTypeStrings)
                .skinConcerns(skinConcernStrings)
                .pouch(pouchDtos) // 등록된 게 없으면 자연스럽게 빈 리스트([])가 담깁니다!
                .build();
    }

    @Transactional
    public void updateMypageProfile(ProfileUpdateRequest request) {
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        BaseAirport airport = request.getBaseAirport() != null ?
                BaseAirport.valueOf(request.getBaseAirport()) : user.getBaseAirport();

        Set<SkinType> skinTypes = new HashSet<>();
        if (request.getSkinType() != null) {
            skinTypes.add(SkinType.valueOf(request.getSkinType()));
        }

        Set<SkinConcern> concerns = new HashSet<>();
        if (request.getSkinConcerns() != null) {
            concerns = request.getSkinConcerns().stream()
                    .map(SkinConcern::valueOf)
                    .collect(Collectors.toSet());
        }

        user.setupProfile(
                request.getName(),
                airport,
                skinTypes,
                concerns,
                request.getProcedureHistory().isHasHistory(),
                request.getProcedureHistory().getDetail(),
                request.getProcedureHistory().isRecentOneMonth()
        );
    }

    @Transactional
    public void updateSunscreen(Long productId, com.hackthon.hackathon.dto.SunscreenUpdateRequest request) {
        Sunscreen sunscreen = sunscreenRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선크림입니다."));

        sunscreen.updateSunscreen(
                request.getBrand(),
                request.getName(),
                request.getFilterType() != null ? com.hackthon.hackathon.enums.SunscreenFilterType.valueOf(request.getFilterType()) : sunscreen.getFilterType(),
                request.getProductType() != null ? com.hackthon.hackathon.enums.SunscreenProductType.valueOf(request.getProductType()) : sunscreen.getProductType(),
                request.getSpf(),
                request.getPa() != null ? com.hackthon.hackathon.enums.Pa.valueOf(request.getPa()) : sunscreen.getPa()
        );
    }

    @Transactional
    public void deleteSunscreen(Long productId) {
        Sunscreen sunscreen = sunscreenRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 선크림입니다."));

        sunscreenRepository.delete(sunscreen);
    }

    @org.springframework.transaction.annotation.Transactional
    public void addProcedure(ProcedureDto.Request request) {
        // 현재 하드코딩된 1번 유저 사용
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        ProcedureHistory procedure = ProcedureHistory.builder()
                .user(user)
                .name(request.getName())
                .isRecentOneMonth(request.isRecentOneMonth())
                .build();

        procedureHistoryRepository.save(procedure);
    }

    // 2. 조회
    public List<ProcedureDto.Response> getProcedures() {
        return procedureHistoryRepository.findByUserId(1L).stream()
                .map(p -> ProcedureDto.Response.builder()
                        .procedureId(p.getId())
                        .name(p.getName())
                        .isRecentOneMonth(p.isRecentOneMonth())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    // 3. 삭제
    @org.springframework.transaction.annotation.Transactional
    public void deleteProcedure(Long procedureId) {
        procedureHistoryRepository.deleteById(procedureId);
    }
}