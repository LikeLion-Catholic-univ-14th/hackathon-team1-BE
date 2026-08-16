package com.hackthon.hackathon.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TimeZoneUtil {

    private TimeZoneUtil() {
    }

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
}