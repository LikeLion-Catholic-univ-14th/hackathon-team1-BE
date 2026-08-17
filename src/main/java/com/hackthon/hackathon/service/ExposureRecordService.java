package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.ExposureRecord;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.Sunscreen;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.LocationType;
import com.hackthon.hackathon.repository.ExposureRecordRepository;
import com.hackthon.hackathon.repository.SunscreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExposureRecordService {

    private final ExposureRecordRepository exposureRecordRepository;
    private final SunscreenRepository sunscreenRepository;

    // ==========================================
    // 일정 있는 날
    // ==========================================

    @Transactional
    public void saveOrUpdate(
            Schedule schedule,
            String airportCode,
            LocalDate date,
            boolean outing,
            ExposureCalculationService.ExposureResult result,
            String weatherCondition
    ) {

        if (result.sunlightStart() == null
                || result.sunlightEnd() == null) {
            return;
        }

        ExposureRecord record =
                exposureRecordRepository
                        .findByScheduleAndDateAndLocationType(
                                schedule,
                                date,
                                LocationType.ARRIVAL
                        )
                        .orElse(null);


        if (record == null) {

            record =
                    ExposureRecord.builder()
                            .user(schedule.getUser())
                            .schedule(schedule)
                            .airportCode(airportCode)
                            .locationType(LocationType.ARRIVAL)
                            .uvIndex(result.maxUv())
                            .date(date)
                            .temperature(result.temperature())
                            .requiredSpf(result.requiredSpf())
                            .riskLevel(result.riskLevel())
                            .sunlightStart(result.sunlightStart())
                            .averageUv(result.averageUv())
                            .sunlightEnd(result.sunlightEnd())
                            .sunlightMinutes(result.sunlightMinutes())

                            // 핵심: schedule.isOuting() 사용 X
                            .isOuting(outing)

                            .estimatedExposureScore(
                                    result.estimatedExposureScore()
                            )
                            .koreaComparison(
                                    result.koreaComparison()
                            )
                            .weatherCondition(
                                    weatherCondition
                            )
                            .build();

            exposureRecordRepository.save(
                    record
            );

            return;
        }


        record.updateCalculation(
                airportCode,
                result.maxUv(),
                result.temperature(),
                result.requiredSpf(),
                result.riskLevel(),
                result.sunlightStart(),
                result.averageUv(),
                result.sunlightEnd(),
                result.sunlightMinutes(),

                // 핵심
                outing,

                result.estimatedExposureScore(),
                result.koreaComparison(),
                weatherCondition
        );
    }


    // ==========================================
    // 일정 없는 날
    // ==========================================

    @Transactional
    public void saveOrUpdateBaseDay(
            User user,
            String airportCode,
            LocalDate date,
            boolean outing,
            ExposureCalculationService.ExposureResult result,
            String weatherCondition
    ) {

        if (result.sunlightStart() == null
                || result.sunlightEnd() == null) {
            return;
        }

        ExposureRecord record =
                exposureRecordRepository
                        .findByUserAndDateAndLocationTypeAndScheduleIsNull(
                                user,
                                date,
                                LocationType.ARRIVAL
                        )
                        .orElse(null);


        if (record == null) {

            record =
                    ExposureRecord.builder()
                            .user(user)
                            .schedule(null)
                            .airportCode(airportCode)
                            .locationType(LocationType.ARRIVAL)
                            .uvIndex(result.maxUv())
                            .date(date)
                            .temperature(result.temperature())
                            .requiredSpf(result.requiredSpf())
                            .riskLevel(result.riskLevel())
                            .sunlightStart(result.sunlightStart())
                            .averageUv(result.averageUv())
                            .sunlightEnd(result.sunlightEnd())
                            .sunlightMinutes(result.sunlightMinutes())
                            .isOuting(outing)
                            .estimatedExposureScore(
                                    result.estimatedExposureScore()
                            )
                            .koreaComparison(
                                    result.koreaComparison()
                            )
                            .weatherCondition(
                                    weatherCondition
                            )
                            .build();

            exposureRecordRepository.save(
                    record
            );

            return;
        }


        record.updateCalculation(
                airportCode,
                result.maxUv(),
                result.temperature(),
                result.requiredSpf(),
                result.riskLevel(),
                result.sunlightStart(),
                result.averageUv(),
                result.sunlightEnd(),
                result.sunlightMinutes(),
                outing,
                result.estimatedExposureScore(),
                result.koreaComparison(),
                weatherCondition
        );
    }


    // ==========================================
    // 날짜별 외출 상태 동기화
    // ==========================================

    @Transactional
    public void updateOutingByDate(
            User user,
            LocalDate date,
            boolean outing
    ) {

        exposureRecordRepository
                .findByUserAndDate(
                        user,
                        date
                )
                .forEach(record ->
                        record.updateOuting(
                                outing
                        )
                );
    }

    @Transactional
    public void applySunscreenByDate(
            User user,
            LocalDate date,
            Long sunscreenId,
            boolean applied
    ) {

        Sunscreen sunscreen =
                sunscreenRepository.findById(sunscreenId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 선크림을 찾을 수 없습니다."
                                )
                        );

        List<ExposureRecord> records =
                exposureRecordRepository.findByUserAndDate(
                        user,
                        date
                );

        if (records.isEmpty()) {
            throw new IllegalArgumentException(
                    "해당 날짜의 노출 기록을 찾을 수 없습니다."
            );
        }

        records.forEach(record ->
                record.applySunscreen(
                        sunscreen,
                        applied
                )
        );
    }


    // 기존 코드 호환용
    @Transactional
    public void updateOuting(
            Schedule schedule,
            boolean outing
    ) {

        exposureRecordRepository
                .findBySchedule(schedule)
                .forEach(record ->
                        record.updateOuting(
                                outing
                        )
                );
    }


    @Transactional
    public void updateBaseDayOuting(
            User user,
            LocalDate date,
            boolean outing
    ) {

        exposureRecordRepository
                .findByUserAndDateAndLocationTypeAndScheduleIsNull(
                        user,
                        date,
                        LocationType.ARRIVAL
                )
                .ifPresent(record ->
                        record.updateOuting(
                                outing
                        )
                );
    }

}