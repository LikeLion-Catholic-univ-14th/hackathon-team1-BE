package com.hackthon.hackathon.service;
import com.hackthon.hackathon.util.AirportLocationMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class TimeConversionService {
    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    // 일정표에 적힌 현지시간 → UTC
    public Instant localToUtc(
            LocalDateTime localDateTime,
            String airportCode
    ) {

        ZoneId localZone =
                AirportLocationMapper.getZoneId(airportCode);

        return localDateTime
                .atZone(localZone)
                .toInstant();
    }

    // UTC → 목적지 현지시간
    public ZonedDateTime utcToLocal(
            Instant utcTime,
            String airportCode
    ) {

        ZoneId localZone =
                AirportLocationMapper.getZoneId(airportCode);

        return utcTime.atZone(localZone);
    }

    // UTC → 한국시간
    public ZonedDateTime utcToKorea(Instant utcTime) {
        return utcTime.atZone(KOREA_ZONE);
    }
}
