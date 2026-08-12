package com.hackthon.hackathon.util;
import java.util.Map;
public class AirportLocationMapper {
    private static final Map<String, AirportInfo> AIRPORTS = Map.of(
            "ICN", new AirportInfo(
                    "인천",
                    "대한민국",
                    37.4602,
                    126.4407,
                    "Asia/Seoul"
            ),
            "SYD", new AirportInfo(
                    "시드니",
                    "호주",
                    -33.9399,
                    151.1753,
                    "Australia/Sydney"
            ),
            "NRT", new AirportInfo(
                    "도쿄",
                    "일본",
                    35.7720,
                    140.3929,
                    "Asia/Tokyo"
            ),
            "CDG", new AirportInfo(
                    "파리",
                    "프랑스",
                    49.0097,
                    2.5479,
                    "Europe/Paris"
            )
    );

    private AirportLocationMapper() {
    }

    public static AirportInfo getAirportInfo(String airportCode) {

        AirportInfo airportInfo = AIRPORTS.get(airportCode);

        if (airportInfo == null) {
            throw new IllegalArgumentException(
                    "지원하지 않는 공항 코드입니다: " + airportCode
            );
        }

        return airportInfo;
    }
}
