package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.ScheduleDailyResponse;
import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.entity.DailyOuting;
import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.entity.User;
import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.repository.DailyOutingRepository;
import com.hackthon.hackathon.repository.ScheduleRepository;
import com.hackthon.hackathon.repository.UserRepository;
import com.hackthon.hackathon.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ScheduleDailyService {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final DailyOutingRepository dailyOutingRepository;

    private final WeatherService weatherService;
    private final ExposureCalculationService exposureCalculationService;


    // =====================================================
    // 날짜 상세 조회
    // =====================================================

    public ScheduleDailyResponse getDaily(
            Long userId,
            LocalDate date
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 유저를 찾을 수 없습니다."
                                )
                        );


        List<Schedule> schedules =
                scheduleRepository
                        .findByUserOrderByDepartureTimeAsc(
                                user
                        );


        String baseAirportCode =
                convertBaseAirportToAirportCode(
                        user.getBaseAirport()
                );


        Schedule schedule =
                findScheduleForDate(
                        schedules,
                        date,
                        baseAirportCode
                );


        // 일정 없는 날
        if (schedule == null) {

            return createBaseDayResponse(
                    user,
                    date
            );
        }


        return createResponse(
                user,
                schedule,
                date
        );
    }


    // =====================================================
    // 기존 schedule 기반 조회
    // =====================================================

    public ScheduleDailyResponse getDailyBySchedule(
            Schedule schedule
    ) {

        LocalDate arrivalLocalDate =
                getArrivalLocalTime(
                        schedule
                )
                        .toLocalDate();


        return createResponse(
                schedule.getUser(),
                schedule,
                arrivalLocalDate
        );
    }


    public ScheduleDailyResponse getDailyBySchedule(
            Schedule schedule,
            LocalDate date
    ) {

        return createResponse(
                schedule.getUser(),
                schedule,
                date
        );
    }


    // =====================================================
    // 날짜에 해당하는 비행 / 레이오버 찾기
    // =====================================================

    private Schedule findScheduleForDate(
            List<Schedule> schedules,
            LocalDate date,
            String baseAirportCode
    ) {


        // ------------------------------------------
        // 실제 비행 일정
        // ------------------------------------------

        Schedule flightSchedule =
                schedules.stream()
                        .filter(schedule -> {

                            LocalDate departureDate =
                                    getDepartureLocalTime(
                                            schedule
                                    )
                                            .toLocalDate();

                            LocalDate arrivalDate =
                                    getArrivalLocalTime(
                                            schedule
                                    )
                                            .toLocalDate();


                            return !date.isBefore(
                                    departureDate
                            )
                                    && !date.isAfter(
                                    arrivalDate
                            );
                        })
                        .min(
                                Comparator.comparing(
                                        Schedule::getDepartureTime
                                )
                        )
                        .orElse(null);


        if (flightSchedule != null) {
            return flightSchedule;
        }


        // ------------------------------------------
        // 레이오버
        // ------------------------------------------

        for (Schedule current : schedules) {

            LocalDate arrivalDate =
                    getArrivalLocalTime(
                            current
                    )
                            .toLocalDate();


            if (date.isBefore(
                    arrivalDate
            )) {
                continue;
            }


            /*
             * 소속공항으로 귀국한 경우
             * 레이오버로 취급 X
             */
            if (current.getArrivalAirport()
                    .equals(
                            baseAirportCode
                    )) {

                continue;
            }


            Schedule nextDeparture =
                    schedules.stream()
                            .filter(next ->
                                    next.getDepartureTime()
                                            .isAfter(
                                                    current.getArrivalTime()
                                            )
                            )
                            .filter(next ->
                                    next.getDepartureAirport()
                                            .equals(
                                                    current.getArrivalAirport()
                                            )
                            )
                            .min(
                                    Comparator.comparing(
                                            Schedule::getDepartureTime
                                    )
                            )
                            .orElse(null);


            if (nextDeparture == null) {
                continue;
            }


            LocalDate nextDepartureDate =
                    getDepartureLocalTime(
                            nextDeparture
                    )
                            .toLocalDate();


            if (!date.isBefore(
                    arrivalDate
            )
                    && date.isBefore(
                    nextDepartureDate
            )) {

                /*
                 * Schedule 객체 자체는
                 * 레이오버의 기준 비행편을 반환하지만,
                 *
                 * outing 상태는 아래 createResponse에서
                 * date 기준 DailyOuting을 사용하므로
                 * 날짜별로 독립적이다.
                 */
                return current;
            }
        }


        return null;
    }


    // =====================================================
    // 소속공항 대기일
    // =====================================================

    private ScheduleDailyResponse createBaseDayResponse(
            User user,
            LocalDate date
    ) {

        /*
         * 일정 없는 날 기본 INDOOR(false)
         */
        boolean isOuting =
                dailyOutingRepository
                        .findByUserAndDate(
                                user,
                                date
                        )
                        .map(
                                DailyOuting::isOuting
                        )
                        .orElse(false);


        String baseAirportCode =
                convertBaseAirportToAirportCode(
                        user.getBaseAirport()
                );


        ScheduleDailyResponse.LocationInfo baseInfo =
                createLocationInfo(
                        baseAirportCode,
                        date
                );


        return new ScheduleDailyResponse(
                null,
                date.toString(),
                baseAirportCode,
                "STAY",
                isOuting,
                null,
                null,
                baseInfo,
                null
        );
    }


    // =====================================================
    // 실제 비행 / 레이오버 응답
    // =====================================================

    private ScheduleDailyResponse createResponse(
            User user,
            Schedule schedule,
            LocalDate date
    ) {

        String flightStatus =
                calculateFlightStatus(
                        schedule,
                        date
                );


        String route =
                schedule.getDepartureAirport()
                        + " → "
                        + schedule.getArrivalAirport();


        /*
         * 핵심:
         *
         * 일정 있는 날은 DailyOuting이 없으면
         * 기본 OUTING(true)
         */
        boolean isOuting =
                dailyOutingRepository
                        .findByUserAndDate(
                                user,
                                date
                        )
                        .map(
                                DailyOuting::isOuting
                        )
                        .orElse(true);


        ScheduleDailyResponse.LocationInfo departureInfo =
                null;

        ScheduleDailyResponse.LocationInfo arrivalInfo =
                null;


        switch (flightStatus) {

            case "SCHEDULED" -> {

                departureInfo =
                        createLocationInfo(
                                schedule.getDepartureAirport(),
                                date
                        );
            }


            case "IN_FLIGHT" -> {

                departureInfo =
                        createLocationInfo(
                                schedule.getDepartureAirport(),
                                date
                        );


                arrivalInfo =
                        createLocationInfo(
                                schedule.getArrivalAirport(),
                                date
                        );
            }


            case "ARRIVED" -> {

                arrivalInfo =
                        createLocationInfo(
                                schedule.getArrivalAirport(),
                                date
                        );
            }
        }


        /*
         * DB UTC → 각 공항 현지시간
         */
        LocalDateTime departureLocalTime =
                getDepartureLocalTime(
                        schedule
                );


        LocalDateTime arrivalLocalTime =
                getArrivalLocalTime(
                        schedule
                );


        return new ScheduleDailyResponse(
                schedule.getId(),
                date.toString(),
                route,
                flightStatus,
                isOuting,
                departureLocalTime,
                arrivalLocalTime,
                departureInfo,
                arrivalInfo
        );
    }


    // =====================================================
    // 비행 상태
    // =====================================================

    private String calculateFlightStatus(
            Schedule schedule,
            LocalDate date
    ) {

        LocalDate departureDate =
                getDepartureLocalTime(
                        schedule
                )
                        .toLocalDate();


        LocalDate arrivalDate =
                getArrivalLocalTime(
                        schedule
                )
                        .toLocalDate();


        if (date.equals(
                departureDate
        )) {
            return "IN_FLIGHT";
        }


        if (!date.isBefore(
                arrivalDate
        )) {
            return "ARRIVED";
        }


        return "SCHEDULED";
    }


    // =====================================================
    // UTC → 현지시간
    // =====================================================

    private LocalDateTime getDepartureLocalTime(
            Schedule schedule
    ) {

        return TimeZoneUtil.fromUtc(
                schedule.getDepartureTime(),
                schedule.getDepartureAirport()
        );
    }


    private LocalDateTime getArrivalLocalTime(
            Schedule schedule
    ) {

        return TimeZoneUtil.fromUtc(
                schedule.getArrivalTime(),
                schedule.getArrivalAirport()
        );
    }


    // =====================================================
    // 공항별 UV
    // =====================================================

    private ScheduleDailyResponse.LocationInfo createLocationInfo(
            String airportCode,
            LocalDate date
    ) {

        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );


        List<ScheduleDailyResponse.UvPoint> graph =
                createUvGraph(
                        weather,
                        date
                );


        if (graph.isEmpty()) {

            return new ScheduleDailyResponse.LocationInfo(
                    airportCode,
                    calculateKoreaTimeDifference(
                            airportCode,
                            date
                    ),
                    null,
                    new ScheduleDailyResponse.UvDetail(
                            List.of(),
                            null
                    )
            );
        }


        double maxUv =
                graph.stream()
                        .mapToDouble(
                                ScheduleDailyResponse.UvPoint::uvValue
                        )
                        .max()
                        .orElse(0.0);


        String warningMessage =
                createWarningMessage(
                        maxUv
                );


        return new ScheduleDailyResponse.LocationInfo(
                airportCode,
                calculateKoreaTimeDifference(
                        airportCode,
                        date
                ),
                exposureCalculationService
                        .calculateRiskLevel(
                                maxUv
                        ),
                new ScheduleDailyResponse.UvDetail(
                        graph,
                        warningMessage
                )
        );
    }


    private List<ScheduleDailyResponse.UvPoint> createUvGraph(
            WeatherResponse weather,
            LocalDate date
    ) {

        if (weather == null
                || weather.getHourly() == null
                || weather.getHourly().getTime() == null
                || weather.getHourly().getUvIndex() == null) {

            return List.of();
        }


        List<String> times =
                weather.getHourly()
                        .getTime();


        List<Double> uvIndexes =
                weather.getHourly()
                        .getUvIndex();


        int size =
                Math.min(
                        times.size(),
                        uvIndexes.size()
                );


        return IntStream
                .range(
                        0,
                        size
                )
                .mapToObj(i -> {

                    LocalDateTime time =
                            LocalDateTime.parse(
                                    times.get(i)
                            );


                    Double uv =
                            uvIndexes.get(i);


                    if (!time.toLocalDate()
                            .equals(
                                    date
                            )
                            || uv == null) {

                        return null;
                    }


                    return new ScheduleDailyResponse.UvPoint(
                            time.toLocalTime()
                                    .toString(),
                            uv
                    );
                })
                .filter(
                        Objects::nonNull
                )
                .toList();
    }


    private String createWarningMessage(
            double maxUv
    ) {

        if (maxUv >= 8) {
            return "자외선이 매우 강한 날입니다.";
        }


        if (maxUv >= 5) {
            return "자외선 노출에 주의가 필요합니다.";
        }


        return "자외선 위험이 낮은 편입니다.";
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


    private String calculateKoreaTimeDifference(
            String airportCode,
            LocalDate date
    ) {

        String timezone =
                com.hackthon.hackathon.util.AirportLocationMapper
                        .getAirportInfo(
                                airportCode
                        )
                        .timezone();


        java.time.ZoneId koreaZone =
                java.time.ZoneId.of(
                        "Asia/Seoul"
                );


        java.time.ZoneId localZone =
                java.time.ZoneId.of(
                        timezone
                );


        java.time.ZonedDateTime koreaTime =
                date.atStartOfDay(
                        koreaZone
                );


        java.time.ZonedDateTime localTime =
                date.atStartOfDay(
                        localZone
                );


        int koreaOffsetSeconds =
                koreaTime.getOffset()
                        .getTotalSeconds();


        int localOffsetSeconds =
                localTime.getOffset()
                        .getTotalSeconds();


        int differenceHours =
                (localOffsetSeconds
                        - koreaOffsetSeconds)
                        / 3600;


        if (differenceHours > 0) {

            return "+"
                    + differenceHours
                    + "시간";
        }


        return differenceHours
                + "시간";
    }
}