package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.ExposureRecord;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.LocationType;
import com.hackthon.hackathon.repository.ExposureRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExposureRecordService {

    private final ExposureRecordRepository exposureRecordRepository;

    /**
     * 비행/레이오버 일정이 있는 날
     */
    @Transactional
    public void saveOrUpdate(
            Schedule schedule,
            String airportCode,
            LocalDate date,
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
                            .isOuting(schedule.isOuting())
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

            exposureRecordRepository.save(record);

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
                schedule.isOuting(),
                result.estimatedExposureScore(),
                result.koreaComparison(),
                weatherCondition
        );
    }

    /**
     * 비행 일정이 없는 소속공항 대기일
     *
     * schedule = null
     * user + date 기준으로 기록
     */
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

                            // 일정 없는 날이므로 null
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

            exposureRecordRepository.save(record);

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

    /**
     * 일정 있는 날의 외출 상태 변경
     */
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

    /**
     * 일정 없는 날의 외출 상태 변경
     */
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