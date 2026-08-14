package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.dto.home.HomeUvResponse;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.BaseAirport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseDayExposureService {

    private final WeatherService weatherService;
    private final SunlightWindowService sunlightWindowService;
    private final ExposureCalculationService exposureCalculationService;
    private final HomeUvService homeUvService;
    private final ExposureRecordService exposureRecordService;

    @Transactional
    public void createOrUpdate(
            User user,
            LocalDate date,
            boolean outing
    ) {

        /*
         * 미래 날짜는 실제 노출 기록으로 저장하지 않음
         */
        if (date.isAfter(LocalDate.now())) {
            return;
        }

        String airportCode =
                convertBaseAirportToAirportCode(
                        user.getBaseAirport()
                );


        // ==========================================
        // 1. 소속공항 날씨
        // ==========================================

        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );


        // ==========================================
        // 2. 대기일 외출 가능 시간
        // ==========================================

        SunlightWindowService.AvailableWindow availableWindow =
                sunlightWindowService
                        .calculateBaseDayAvailableWindow(
                                date
                        );


        // ==========================================
        // 3. 햇빛창
        // ==========================================

        List<SunlightWindowService.SunlightWindow> windows =
                sunlightWindowService
                        .calculateSunlightWindows(
                                availableWindow,
                                weather
                        );


        /*
         * 해당 날짜의 날씨 데이터가 없거나
         * 일조시간 계산이 불가능하면 저장하지 않음
         */
        if (windows.isEmpty()) {
            return;
        }


        // ==========================================
        // 4. 서울 비교 기준
        // ==========================================

        WeatherResponse seoulWeather =
                weatherService.getSeoulWeather();

        int sunlightMinutes =
                windows.stream()
                        .mapToInt(window ->
                                (int) window.minutes()
                        )
                        .sum();


        double seoulComparableExposureScore =
                exposureCalculationService
                        .calculateSeoulComparableExposureScore(
                                seoulWeather,
                                sunlightMinutes
                        );


        // ==========================================
        // 5. 노출량 계산
        // ==========================================

        ExposureCalculationService.ExposureResult exposureResult =
                exposureCalculationService
                        .calculateExposure(
                                windows,
                                weather,
                                seoulComparableExposureScore
                        );


        // ==========================================
        // 6. 날씨 condition 얻기
        // ==========================================

        HomeUvResponse homeResponse =
                homeUvService.createTestHomeUv(
                        user.getId(),
                        airportCode,
                        exposureResult,
                        windows
                );


        // ==========================================
        // 7. ExposureRecord 생성 / 갱신
        // ==========================================

        exposureRecordService
                .saveOrUpdateBaseDay(
                        user,
                        airportCode,
                        date,
                        outing,
                        exposureResult,
                        homeResponse.weatherCondition()
                );
    }


    private String convertBaseAirportToAirportCode(
            BaseAirport baseAirport
    ) {

        if (baseAirport == null) {
            throw new IllegalStateException(
                    "사용자의 소속 공항이 등록되어 있지 않습니다."
            );
        }

        return switch (baseAirport) {
            case INCHEON -> "ICN";
            case GIMPO -> "GMP";
        };
    }
}