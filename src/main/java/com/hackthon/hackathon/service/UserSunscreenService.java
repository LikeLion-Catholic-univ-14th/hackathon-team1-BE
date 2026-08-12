package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.repository.SunscreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSunscreenService {

    private final SunscreenRepository sunscreenRepository;
    private final SunscreenCalculationService sunscreenCalculationService;

    public List<SunscreenProtectionResponse> calculateUserSunscreens(
            Long userId,
            double uvIndex
    ) {

        List<Sunscreen> sunscreens =
                sunscreenRepository.findByUserId(userId);

        return sunscreens.stream()
                .map(sunscreen -> {

                    SunscreenCalculationService.ProtectionResult result =
                            sunscreenCalculationService.evaluateProtection(
                                    sunscreen.getSpf(),
                                    sunscreen.getProductType(),
                                    uvIndex
                            );

                    return new SunscreenProtectionResponse(
                            sunscreen.getId(),
                            sunscreen.getBrand(),
                            sunscreen.getName(),
                            sunscreen.getProductType(),
                            sunscreen.getSpf(),
                            result.effectiveSpf(),
                            result.requiredSpf(),
                            result.insufficient()
                    );
                })
                .toList();
    }

    public record SunscreenProtectionResponse(
            Long sunscreenId,
            String brand,
            String name,
            com.hackthon.hackathon.enums.SunscreenProductType productType,
            String displayedSpf,
            double effectiveSpf,
            int requiredSpf,
            boolean insufficient
    ) {
    }

    public SunscreenProtectionResponse recommendSunscreen(
            Long userId,
            double uvIndex
    ) {


        List<SunscreenProtectionResponse> results =
                calculateUserSunscreens(userId, uvIndex);

        // 1. 필요 SPF를 충족하는 제품 중
        // 필요한 SPF와 가장 가까운 제품 선택
        return results.stream()
                .filter(result -> !result.insufficient())
                .min(
                        java.util.Comparator.comparingDouble(
                                result ->
                                        result.effectiveSpf()
                                                - result.requiredSpf()
                        )
                )

                // 2. 전부 부족하다면 그중 실효 SPF가 가장 높은 제품
                .orElseGet(() ->
                        results.stream()
                                .max(
                                        java.util.Comparator.comparingDouble(
                                                SunscreenProtectionResponse::effectiveSpf
                                        )
                                )
                                .orElse(null)
                );
    }

}