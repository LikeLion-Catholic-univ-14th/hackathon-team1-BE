package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.ExposureRecord;
import com.hackthon.hackathon.entity.Schedule;
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

    @Transactional
    public void saveOrUpdate(
            Schedule schedule,
            String airportCode,
            LocalDate date,
            ExposureCalculationService.ExposureResult result,
            String weatherCondition
    ) {

        /*
         * 햇빛창 자체가 없는 경우에는
         * ExposureRecord의 sunlightStart/end가 NOT NULL이라
         * 저장하지 않는다.
         */
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
                            .uvIndex(
                                            result.maxUv()
                            )
                            .date(date)
                            .temperature(
                                    result.temperature()
                            )
                            .requiredSpf(
                                    result.requiredSpf()
                            )
                            .riskLevel(
                                    result.riskLevel()
                            )
                            .sunlightStart(
                                    result.sunlightStart()
                            )
                            .averageUv(
                                    result.averageUv()
                            )
                            .sunlightEnd(
                                    result.sunlightEnd()
                            )
                            .sunlightMinutes(
                                    result.sunlightMinutes()
                            )
                            .isOuting(
                                    schedule.isOuting()
                            )
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

        // 이미 있으면 중복 INSERT하지 않고 최신 계산값으로 갱신
        record.updateCalculation(
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
}