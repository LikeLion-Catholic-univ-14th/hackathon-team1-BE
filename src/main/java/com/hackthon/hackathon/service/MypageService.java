package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.repository.ScheduleRepository;
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

@Service
@RequiredArgsConstructor
public class MypageService {
    private final SunscreenRepository sunscreenRepository;
    private final UserRepository userRepository;
    private final com.hackthon.hackathon.repository.ProcedureHistoryRepository procedureHistoryRepository;
    private final ScheduleRepository scheduleRepository;

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


        List<String> skinTypeStrings = user.getSkinTypes() != null ?
                user.getSkinTypes().stream().map(Enum::name).collect(Collectors.toList()) : null;

        List<String> skinConcernStrings = user.getSkinConcerns() != null ?
                user.getSkinConcerns().stream().map(Enum::name).collect(Collectors.toList()) : null;


        MypageResponse.ProcedureHistoryDto procedureDto = MypageResponse.ProcedureHistoryDto.builder()
                .hasHistory(user.isHasProcedureHistory())
                .detail(user.getProcedureDetails())
                .isRecentOneMonth(user.getProcedureWithinOneMonth())
                .build();


        return MypageResponse.builder()
                .name(user.getName())
                .baseAirport(user.getBaseAirport() != null ? user.getBaseAirport().name() : null)
                .skinTypes(skinTypeStrings)
                .skinConcerns(skinConcernStrings)
                .procedureHistory(procedureDto) // 최종 응답에 시술 이력 바구니 추가!
                .pouch(pouchDtos)
                .build();
    }

    @Transactional
    public void updateMypageProfile(ProfileUpdateRequest request) {
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        BaseAirport airport = request.getBaseAirport() != null ?
                BaseAirport.valueOf(request.getBaseAirport()) : user.getBaseAirport();

        Set<SkinType> skinTypes = new HashSet<>();
        if (request.getSkinTypes() != null && !request.getSkinTypes().isEmpty()) {
            skinTypes = request.getSkinTypes().stream()
                    .map(SkinType::valueOf)
                    .collect(Collectors.toSet());
        }

        Set<SkinConcern> concerns = new HashSet<>();
        if (request.getSkinConcerns() != null && !request.getSkinConcerns().isEmpty()) {
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

        List<Schedule> schedules = scheduleRepository.findBySelectedSunscreen(sunscreen);

        for (Schedule schedule : schedules) {
            schedule.removeSelectedSunscreen();
        }

        sunscreenRepository.delete(sunscreen);
    }

    @org.springframework.transaction.annotation.Transactional
    public void addProcedure(ProcedureDto.Request request) {
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        ProcedureHistory procedure = ProcedureHistory.builder()
                .user(user)
                .name(request.getName())
                .isRecentOneMonth(request.isRecentOneMonth())
                .build();

        procedureHistoryRepository.save(procedure);
    }

    //조회
    public List<ProcedureDto.Response> getProcedures() {
        return procedureHistoryRepository.findByUserId(1L).stream()
                .map(p -> ProcedureDto.Response.builder()
                        .procedureId(p.getId())
                        .name(p.getName())
                        .isRecentOneMonth(p.isRecentOneMonth())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    //삭제
    @org.springframework.transaction.annotation.Transactional
    public void deleteProcedure(Long procedureId) {
        procedureHistoryRepository.deleteById(procedureId);
    }
}