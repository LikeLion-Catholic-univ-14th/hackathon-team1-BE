package com.hackthon.hackathon.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TimeZoneUtil {

    private TimeZoneUtil() {
    }

    // ==========================================
    // 공항 현지시간 → UTC
    // ==========================================

    public static LocalDateTime toUtc(
            LocalDateTime localDateTime,
            String airportCode
    ) {

        AirportInfo airportInfo =
                AirportLocationMapper.getAirportInfo(
                        airportCode
                );

        ZoneId airportZone =
                ZoneId.of(
                        airportInfo.timezone()
                );

        return localDateTime
                .atZone(airportZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }


    // ==========================================
    // UTC → 공항 현지시간
    // ==========================================

    public static LocalDateTime fromUtc(
            LocalDateTime utcDateTime,
            String airportCode
    ) {

        if (utcDateTime == null) {
            return null;
        }

        AirportInfo airportInfo =
                AirportLocationMapper.getAirportInfo(
                        airportCode
                );

        ZoneId airportZone =
                ZoneId.of(
                        airportInfo.timezone()
                );

        return utcDateTime
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(airportZone)
                .toLocalDateTime();
    }
}