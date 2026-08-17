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

    // 추가
    private final SunlightWindowService sunlightWindowService;
    private final ExposureRecordService exposureRecordService;


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


        // =================================================
        // 일정 없는 소속공항 대기일
        // =================================================

        if (schedule == null) {

            ScheduleDailyResponse response =
                    createBaseDayResponse(
                            user,
                            date
                    );


            /*
             * 일정 없는 날도
             * 날짜 상세을 실제 조회했다면
             * ExposureRecord 생성
             */
            saveBaseDayExposure(
                    user,
                    baseAirportCode,
                    date,
                    response.isOuting()
            );


            return response;
        }


        // =================================================
        // 비행 / 레이오버
        // =================================================

        ScheduleDailyResponse response =
                createResponse(
                        user,
                        schedule,
                        date
                );


        /*
         * 날짜 상세 조회 시
         * 그 날짜의 ExposureRecord 생성/갱신
         */
        saveScheduleExposure(
                user,
                schedules,
                schedule,
                date,
                response.isOuting(),
                baseAirportCode
        );


        return response;
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


    // =====================================================
    // 캘린더용
    //
    // 캘린더에서 매일 호출될 수 있으므로
    // 여기서는 DB 저장하지 않는다.
    // =====================================================

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
             * 레이오버 X
             */
            if (isKoreanBaseAirport(
                    current.getArrivalAirport()
            )) {

                continue;
            }


            Schedule nextDeparture =
                    findNextDeparture(
                            schedules,
                            current
                    );


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

                return current;
            }
        }


        return null;
    }


    // =====================================================
    // 다음 출발편 찾기
    // =====================================================

    private Schedule findNextDeparture(
            List<Schedule> schedules,
            Schedule current
    ) {

        return schedules.stream()

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
    }


    // =====================================================
    // 소속공항 대기일 응답
    // =====================================================

    private ScheduleDailyResponse createBaseDayResponse(
            User user,
            LocalDate date
    ) {

        /*
         * 일정 없는 날
         * DailyOuting 없으면 기본 INDOOR
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
         * 일정 있는 날
         *
         * DailyOuting 없으면 기본 OUTING
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
         * DB UTC → 현지시간
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
    // 일정 있는 날 ExposureRecord 생성
    // =====================================================

    private void saveScheduleExposure(
            User user,
            List<Schedule> schedules,
            Schedule schedule,
            LocalDate date,
            boolean outing,
            String baseAirportCode
    ) {

        String flightStatus =
                calculateFlightStatus(
                        schedule,
                        date
                );


        /*
         * 어떤 도시의 자외선 노출을
         * 기록할지 결정
         */
        String airportCode;


        if ("ARRIVED".equals(
                flightStatus
        )) {

            // 레이오버
            airportCode =
                    schedule.getArrivalAirport();

        } else if ("IN_FLIGHT".equals(
                flightStatus
        )) {

            /*
             * 비행일은 일단 도착지를
             * 노출 도시 기준으로 사용
             */
            airportCode =
                    schedule.getArrivalAirport();

        } else {

            // 출발 전
            airportCode =
                    schedule.getDepartureAirport();
        }


        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );


        SunlightWindowService.AvailableWindow availableWindow;


        // ------------------------------------------
        // 레이오버
        // ------------------------------------------

        if ("ARRIVED".equals(
                flightStatus
        )) {

            Schedule nextDeparture =
                    findNextDeparture(
                            schedules,
                            schedule
                    );


            LocalDateTime arrivalLocal =
                    getArrivalLocalTime(
                            schedule
                    );


            LocalDateTime nextDepartureLocal =
                    nextDeparture != null
                            ? getDepartureLocalTime(
                            nextDeparture
                    )
                            : null;


            /*
             * 전체 레이오버가 며칠이어도
             * 조회 날짜 하루로 잘라서 계산한다.
             */
            LocalDateTime dayStart =
                    date.atStartOfDay();


            LocalDateTime dayEnd =
                    date.atTime(
                            23,
                            59,
                            59
                    );


            LocalDateTime effectiveStart =
                    arrivalLocal.isAfter(
                            dayStart
                    )
                            ? arrivalLocal
                            : dayStart;


            LocalDateTime effectiveEnd;


            if (nextDepartureLocal == null) {

                effectiveEnd =
                        dayEnd;

            } else {

                effectiveEnd =
                        nextDepartureLocal.isBefore(
                                dayEnd
                        )
                                ? nextDepartureLocal
                                : dayEnd;
            }


            /*
             * 당일 계산 가능한 구간이 없으면 저장 X
             */
            if (!effectiveEnd.isAfter(
                    effectiveStart
            )) {
                return;
            }


            availableWindow =
                    sunlightWindowService
                            .calculateAvailableWindow(
                                    effectiveStart,
                                    effectiveEnd,
                                    schedule.isQuickTurn()
                            )
                            .orElse(null);

        } else {

            /*
             * 출발일 / 비행일
             *
             * 우선 해당 날짜 하루 기준으로
             * 햇빛창을 계산.
             *
             * 비행일 세부 노출은 기존 Today 계산과
             * 완전히 같은 정책으로 만들려면
             * 별도 로직이 필요하지만,
             * 월말 연결을 위해 날짜 단위 record 생성.
             */
            availableWindow =
                    sunlightWindowService
                            .calculateBaseDayAvailableWindow(
                                    date
                            );
        }


        if (availableWindow == null) {
            return;
        }


        List<SunlightWindowService.SunlightWindow> windows =
                sunlightWindowService
                        .calculateSunlightWindows(
                                availableWindow,
                                weather
                        );


        if (windows.isEmpty()) {
            return;
        }


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


        ExposureCalculationService.ExposureResult exposureResult =
                exposureCalculationService
                        .calculateExposure(
                                windows,
                                weather,
                                seoulComparableExposureScore
                        );


        /*
         * 월말 리포트가 읽는 ExposureRecord 생성
         */
        exposureRecordService
                .saveOrUpdate(
                        schedule,
                        airportCode,
                        date,
                        outing,
                        exposureResult,
                        getWeatherCondition(
                                weather, date
                        )
                );
    }


    // =====================================================
    // 일정 없는 날 ExposureRecord 생성
    // =====================================================

    private void saveBaseDayExposure(
            User user,
            String airportCode,
            LocalDate date,
            boolean outing
    ) {

        WeatherResponse weather =
                weatherService.getWeather(
                        airportCode
                );


        SunlightWindowService.AvailableWindow availableWindow =
                sunlightWindowService
                        .calculateBaseDayAvailableWindow(
                                date
                        );


        List<SunlightWindowService.SunlightWindow> windows =
                sunlightWindowService
                        .calculateSunlightWindows(
                                availableWindow,
                                weather
                        );


        if (windows.isEmpty()) {
            return;
        }


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


        ExposureCalculationService.ExposureResult exposureResult =
                exposureCalculationService
                        .calculateExposure(
                                windows,
                                weather,
                                seoulComparableExposureScore
                        );


        exposureRecordService
                .saveOrUpdateBaseDay(
                        user,
                        airportCode,
                        date,
                        outing,
                        exposureResult,
                        getWeatherCondition(
                                weather, date
                        )
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
                                ScheduleDailyResponse
                                        .UvPoint::uvValue
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


    // =====================================================
    // 날씨 condition
    // =====================================================

    private String getWeatherCondition(
            WeatherResponse weather,
            LocalDate date
    ) {

        if (weather == null
                || weather.getHourly() == null
                || weather.getHourly().getTime() == null
                || weather.getHourly().getWeatherCode() == null) {

            return null;
        }

        List<String> times =
                weather.getHourly().getTime();

        List<Integer> codes =
                weather.getHourly().getWeatherCode();

        int size =
                Math.min(
                        times.size(),
                        codes.size()
                );

        /*
         * 해당 날짜의 weather_code 중
         * 낮 시간대 우선으로 대표값 선택
         */
        Integer selectedCode = null;

        for (int i = 0; i < size; i++) {

            String timeString =
                    times.get(i);

            Integer code =
                    codes.get(i);

            if (timeString == null
                    || code == null) {
                continue;
            }

            LocalDateTime time =
                    LocalDateTime.parse(
                            timeString
                    );

            if (!time.toLocalDate()
                    .equals(date)) {
                continue;
            }

            /*
             * 12시 weather_code를 대표값으로 우선 사용
             */
            if (time.getHour() == 12) {

                selectedCode = code;
                break;
            }

            /*
             * 12시가 없을 경우를 대비한 fallback
             */
            if (selectedCode == null) {
                selectedCode = code;
            }
        }

        if (selectedCode == null) {
            return null;
        }

        return convertWeatherCode(
                selectedCode
        );
    }


    private String convertWeatherCode(
            int code
    ) {

        /*
         * Open-Meteo WMO weather code
         */

        if (code == 0) {
            return "CLEAR";
        }

        if (code == 1
                || code == 2
                || code == 3) {

            return "CLOUDY";
        }

        if (code == 45
                || code == 48) {

            return "FOG";
        }

        if ((code >= 51 && code <= 57)
                || (code >= 61 && code <= 67)
                || (code >= 80 && code <= 82)) {

            return "RAIN";
        }

        if ((code >= 71 && code <= 77)
                || (code >= 85 && code <= 86)) {

            return "SNOW";
        }

        if (code >= 95 && code <= 99) {

            return "THUNDERSTORM";
        }

        return "UNKNOWN";
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

            case INCHEON ->
                    "ICN";

            case GIMPO ->
                    "GMP";
        };
    }

    /*
     * 국내 베이스 복귀 판정에서는 ICN과 GMP를 동일한 국내 복귀로 본다.
     * 사용자의 소속이 GMP여도 ICN 도착 후를 해외 레이오버로 처리하지 않는다.
     */
    private boolean isKoreanBaseAirport(
            String airportCode
    ) {
        return "ICN".equalsIgnoreCase(airportCode.trim())
                || "GMP".equalsIgnoreCase(airportCode.trim());
    }


    private String calculateKoreaTimeDifference(
            String airportCode,
            LocalDate date
    ) {

        String timezone =
                com.hackthon.hackathon.util
                        .AirportLocationMapper
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
                koreaTime
                        .getOffset()
                        .getTotalSeconds();


        int localOffsetSeconds =
                localTime
                        .getOffset()
                        .getTotalSeconds();


        int differenceHours =
                (
                        localOffsetSeconds
                                - koreaOffsetSeconds
                )
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
