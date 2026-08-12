package com.hackthon.hackathon.service;
import com.hackthon.hackathon.entity.ExposureRecord;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.enums.LocationType;
import com.hackthon.hackathon.repository.ExposureRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExposureRecordService {
    private final ExposureRecordRepository exposureRecordRepository;

    @Transactional
    public ExposureRecord saveArrivalExposure(
            Schedule schedule,
            ExposureCalculationService.ExposureResult result,
            String weatherCondition
    ) {

        // 햇빛창 없는 결과는 ExposureRecord로 저장하지 않음
        if (result.sunlightStart() == null || result.sunlightEnd() == null) {
            throw new IllegalArgumentException(
                    "햇빛 노출 시간이 없어 ExposureRecord를 저장할 수 없습니다."
            );
        }

        ExposureRecord record = ExposureRecord.builder()
                .user(schedule.getUser())
                .schedule(schedule)

                // 현재 계산은 도착지 체류 기준
                .airportCode(schedule.getArrivalAirport())
                .locationType(LocationType.ARRIVAL)

                .uvIndex((int) Math.round(result.maxUv()))

                // 햇빛창 시작일을 해당 노출 기록의 날짜로 사용
                .date(result.sunlightStart().toLocalDate())

                .temperature(result.temperature())
                .requiredSpf(result.requiredSpf())
                .riskLevel(result.riskLevel())

                .sunlightStart(result.sunlightStart())
                .averageUv(result.averageUv())
                .sunlightEnd(result.sunlightEnd())
                .sunlightMinutes(result.sunlightMinutes())

                .isOuting(true)

                .estimatedExposureScore(
                        result.estimatedExposureScore()
                )
                .koreaComparison(
                        result.koreaComparison()
                )

                .weatherCondition(weatherCondition)

                .build();

        return exposureRecordRepository.save(record);
    }
}
