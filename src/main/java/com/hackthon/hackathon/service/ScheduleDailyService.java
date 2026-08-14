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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
                        .findByUserOrderByDepartureTimeAsc(user);

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


        /*
         * 실제 비행 / 해외 레이오버 일정이 없는 날
         *
         * → 소속공항 대기일
         * → scheduleId = null
         */
        if (schedule == null) {

            return createBaseDayResponse(
                    user,
                    date
            );
        }

        return createResponse(
                schedule,
                date
        );
    }


    // =====================================================
    // 토글 API용
    // =====================================================

    public ScheduleDailyResponse getDailyBySchedule(
            Schedule schedule
    ) {

        LocalDate date =
                schedule.getArrivalTime()
                        .toLocalDate();

        return createResponse(
                schedule,
                date
        );
    }


    // =====================================================
    // 캘린더용
    // =====================================================

    public ScheduleDailyResponse getDailyBySchedule(
            Schedule schedule,
            LocalDate date
    ) {

        return createResponse(
                schedule,
                date
        );
    }


    // =====================================================
    // 날짜에 해당하는 실제 비행 / 해외 레이오버 찾기
    // =====================================================

    private Schedule findScheduleForDate(
            List<Schedule> schedules,
            LocalDate date,
            String baseAirportCode
    ) {

        /*
         * ==========================================
         * 1. 실제 비행이 걸쳐 있는 날짜
         * ==========================================
         *
         * 출발일 <= 조회일 <= 도착일
         *
         * 이 경우에는 소속공항 여부와 관계없이
         * 실제 비행 일정이므로 Schedule 반환
         */
        Schedule flightSchedule =
                schedules.stream()
                        .filter(schedule -> {

                            LocalDate departureDate =
                                    schedule.getDepartureTime()
                                            .toLocalDate();

                            LocalDate arrivalDate =
                                    schedule.getArrivalTime()
                                            .toLocalDate();

                            return !date.isBefore(departureDate)
                                    && !date.isAfter(arrivalDate);
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


        /*
         * ==========================================
         * 2. 해외 레이오버
         * ==========================================
         *
         * 도착 후
         * 같은 공항에서 다음 출발편이 있기 전까지
         *
         * 단,
         * 도착 공항이 사용자의 소속공항이면
         * 레이오버가 아니라 대기일로 판단
         */
        for (Schedule current : schedules) {

            LocalDate arrivalDate =
                    current.getArrivalTime()
                            .toLocalDate();

            if (date.isBefore(arrivalDate)) {
                continue;
            }


            /*
             * 핵심:
             *
             * SYD → ICN 도착 후
             * ICN은 사용자의 소속공항이므로
             * 그 이후 날짜를 레이오버로 잡지 않음.
             */
            if (current.getArrivalAirport()
                    .equals(baseAirportCode)) {

                continue;
            }


            /*
             * 현재 도착 공항에서 출발하는
             * 가장 빠른 다음 일정 찾기
             */
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

            /*
             * 귀국/다음 출발 일정이 없으면
             * 레이오버 기간을 확정할 수 없으므로 제외
             */
            if (nextDeparture == null) {
                continue;
            }


            LocalDate nextDepartureDate =
                    nextDeparture
                            .getDepartureTime()
                            .toLocalDate();


            /*
             * 도착일 이후 ~ 다음 출발일 이전
             *
             * 예:
             *
             * 8/12 ICN → SYD
             * 8/13 SYD 도착
             * 8/14 SYD 체류
             * 8/15 SYD → ICN
             *
             * 8/14 → current 반환
             */
            if (!date.isBefore(arrivalDate)
                    && date.isBefore(nextDepartureDate)) {

                return current;
            }
        }


        /*
         * ==========================================
         * 비행도 아니고 해외 레이오버도 아님
         * ==========================================
         *
         * → 소속공항 대기일
         */
        return null;
    }


    // =====================================================
    // 소속공항 대기일 응답
    // =====================================================

    private ScheduleDailyResponse createBaseDayResponse(
            User user,
            LocalDate date
    ) {

        boolean isOuting =
                dailyOutingRepository
                        .findByUserAndDate(
                                user,
                                date
                        )
                        .map(DailyOuting::isOuting)
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
                baseInfo,
                null
        );
    }


    // =====================================================
    // 실제 비행 / 레이오버 응답
    // =====================================================

    private ScheduleDailyResponse createResponse(
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

        ScheduleDailyResponse.LocationInfo departureInfo =
                null;

        ScheduleDailyResponse.LocationInfo arrivalInfo =
                null;


        switch (flightStatus) {

            /*
             * 출발 전
             */
            case "SCHEDULED" -> {

                departureInfo =
                        createLocationInfo(
                                schedule.getDepartureAirport(),
                                date
                        );
            }


            /*
             * 비행일
             *
             * 출발지 + 도착지 둘 다
             */
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


            /*
             * 도착 후 / 해외 레이오버
             */
            case "ARRIVED" -> {

                arrivalInfo =
                        createLocationInfo(
                                schedule.getArrivalAirport(),
                                date
                        );
            }
        }


        return new ScheduleDailyResponse(
                schedule.getId(),
                date.toString(),
                route,
                flightStatus,
                schedule.isOuting(),
                departureInfo,
                arrivalInfo
        );
    }


    // =====================================================
    // 조회 날짜 기준 비행 상태
    // =====================================================

    private String calculateFlightStatus(
            Schedule schedule,
            LocalDate date
    ) {

        LocalDate departureDate =
                schedule.getDepartureTime()
                        .toLocalDate();

        LocalDate arrivalDate =
                schedule.getArrivalTime()
                        .toLocalDate();


        /*
         * 출발일
         *
         * 날짜 상세 화면에서는 비행일로 취급
         */
        if (date.equals(departureDate)) {
            return "IN_FLIGHT";
        }


        /*
         * 도착일 및 이후
         */
        if (!date.isBefore(arrivalDate)) {
            return "ARRIVED";
        }


        /*
         * 출발 이전
         */
        return "SCHEDULED";
    }


    // =====================================================
    // 공항별 UV 정보 생성
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


        /*
         * UV 데이터가 없는 날짜
         *
         * SAFE로 오판하지 않고 null 처리
         */
        if (graph.isEmpty()) {

            return new ScheduleDailyResponse.LocationInfo(
                    airportCode,
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


    // =====================================================
    // UV 그래프 생성
    // =====================================================

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
                .range(0, size)
                .mapToObj(i -> {

                    java.time.LocalDateTime time =
                            java.time.LocalDateTime
                                    .parse(
                                            times.get(i)
                                    );

                    Double uv =
                            uvIndexes.get(i);


                    if (!time.toLocalDate()
                            .equals(date)
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
    // UV 경고 메시지
    // =====================================================

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


    // =====================================================
    // 소속공항 → IATA
    // =====================================================

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