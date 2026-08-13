package com.hackthon.hackathon.service;

import com.hackthon.hackathon.enums.SunscreenProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SunscreenCalculationService {

    private final ExposureCalculationService exposureCalculationService;

    public double getApplicationFactor(
            SunscreenProductType productType
    ) {
        return switch (productType) {
            case CREAM -> 0.3;
            case SPRAY -> 0.2;
            case STICK -> 0.4;
        };
    }

    public int parseSpf(String spf) {

        if (spf == null || spf.isBlank()) {
            return 0;
        }

        String numberOnly =
                spf.replaceAll("[^0-9]", "");

        if (numberOnly.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(numberOnly);
    }

    public double calculateEffectiveSpf(
            String displayedSpf,
            SunscreenProductType productType
    ) {

        int spf = parseSpf(displayedSpf);

        return spf * getApplicationFactor(productType);
    }

    public ProtectionResult evaluateProtection(
            String displayedSpf,
            SunscreenProductType productType,
            double uvIndex
    ) {

        double effectiveSpf =
                calculateEffectiveSpf(
                        displayedSpf,
                        productType
                );

        int requiredSpf =
                exposureCalculationService
                        .calculateRequiredSpf(uvIndex);

        boolean insufficient =
                effectiveSpf < requiredSpf;

        return new ProtectionResult(
                effectiveSpf,
                requiredSpf,
                insufficient
        );
    }

    public record ProtectionResult(
            double effectiveSpf,
            int requiredSpf,
            boolean insufficient
    ) {
    }
}