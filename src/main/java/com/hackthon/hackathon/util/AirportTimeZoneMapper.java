package com.hackthon.hackathon.util;
import java.time.ZoneId;
import java.util.Map;
public class AirportTimeZoneMapper {
    private static final Map<String, String> AIRPORT_TIME_ZONES = Map.of(
            "ICN", "Asia/Seoul",
            "SYD", "Australia/Sydney",
            "NRT", "Asia/Tokyo",
            "CDG", "Europe/Paris"
    );
    private AirportTimeZoneMapper() {
    }

    public static ZoneId getZoneId(String airportCode) {

        String timezone = AIRPORT_TIME_ZONES.get(airportCode);

        if (timezone == null) {
            throw new IllegalArgumentException(
                    "지원하지 않는 공항 코드입니다: " + airportCode
            );
        }

        return ZoneId.of(timezone);
    }
}
