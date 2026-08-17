package com.hackthon.hackathon.service;

import com.hackthon.hackathon.entity.Schedule;
import com.hackthon.hackathon.util.TimeZoneUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ScheduleDateResolverService {

    public Schedule findScheduleForDate(
            List<Schedule> schedules,
            LocalDate date
    ) {
        Schedule flightSchedule =
                schedules.stream()
                        .filter(schedule -> {
                            LocalDate departureDate =
                                    TimeZoneUtil.fromUtc(
                                            schedule.getDepartureTime(),
                                            schedule.getDepartureAirport()
                                    ).toLocalDate();

                            LocalDate arrivalDate =
                                    TimeZoneUtil.fromUtc(
                                            schedule.getArrivalTime(),
                                            schedule.getArrivalAirport()
                                    ).toLocalDate();

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

        for (Schedule current : schedules) {
            LocalDate arrivalDate =
                    TimeZoneUtil.fromUtc(
                            current.getArrivalTime(),
                            current.getArrivalAirport()
                    ).toLocalDate();

            if (date.isBefore(arrivalDate)
                    || isKoreanAirport(current.getArrivalAirport())) {
                continue;
            }

            Schedule nextDeparture =
                    findNextDeparture(schedules, current);

            if (nextDeparture == null) {
                continue;
            }

            LocalDate nextDepartureDate =
                    TimeZoneUtil.fromUtc(
                            nextDeparture.getDepartureTime(),
                            nextDeparture.getDepartureAirport()
                    ).toLocalDate();

            if (!date.isBefore(arrivalDate)
                    && date.isBefore(nextDepartureDate)) {
                return current;
            }
        }

        return null;
    }

    public Schedule findNextDeparture(
            List<Schedule> schedules,
            Schedule current
    ) {
        return schedules.stream()
                .filter(next ->
                        next.getDepartureTime()
                                .isAfter(current.getArrivalTime())
                )
                .filter(next ->
                        next.getDepartureAirport()
                                .equals(current.getArrivalAirport())
                )
                .min(
                        Comparator.comparing(
                                Schedule::getDepartureTime
                        )
                )
                .orElse(null);
    }

    public boolean isKoreanAirport(
            String airportCode
    ) {
        if (airportCode == null) {
            return false;
        }

        String normalized =
                airportCode.trim().toUpperCase();

        return "ICN".equals(normalized)
                || "GMP".equals(normalized);
    }
}
