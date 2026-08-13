package com.hackthon.hackathon.util;

import java.util.Map;

public class AirportLocationMapper {

    private static final Map<String, AirportInfo> AIRPORTS = Map.ofEntries(

            // 대한민국
            Map.entry("ICN", new AirportInfo(
                    "인천",
                    "대한민국",
                    37.4602,
                    126.4407,
                    "Asia/Seoul"
            )),

            Map.entry("GMP", new AirportInfo(
                    "서울",
                    "대한민국",
                    37.5583,
                    126.7906,
                    "Asia/Seoul"
            )),

            // 일본
            Map.entry("NRT", new AirportInfo(
                    "도쿄",
                    "일본",
                    35.7720,
                    140.3929,
                    "Asia/Tokyo"
            )),

            Map.entry("HND", new AirportInfo(
                    "도쿄",
                    "일본",
                    35.5494,
                    139.7798,
                    "Asia/Tokyo"
            )),

            Map.entry("KIX", new AirportInfo(
                    "오사카",
                    "일본",
                    34.4347,
                    135.2440,
                    "Asia/Tokyo"
            )),

            // 홍콩 / 대만
            Map.entry("HKG", new AirportInfo(
                    "홍콩",
                    "홍콩",
                    22.3080,
                    113.9185,
                    "Asia/Hong_Kong"
            )),

            Map.entry("TPE", new AirportInfo(
                    "타이베이",
                    "대만",
                    25.0797,
                    121.2342,
                    "Asia/Taipei"
            )),

            // 동남아
            Map.entry("SIN", new AirportInfo(
                    "싱가포르",
                    "싱가포르",
                    1.3644,
                    103.9915,
                    "Asia/Singapore"
            )),

            Map.entry("BKK", new AirportInfo(
                    "방콕",
                    "태국",
                    13.6900,
                    100.7501,
                    "Asia/Bangkok"
            )),

            Map.entry("SGN", new AirportInfo(
                    "호찌민",
                    "베트남",
                    10.8188,
                    106.6519,
                    "Asia/Ho_Chi_Minh"
            )),

            // 호주
            Map.entry("SYD", new AirportInfo(
                    "시드니",
                    "호주",
                    -33.9399,
                    151.1753,
                    "Australia/Sydney"
            )),

            Map.entry("MEL", new AirportInfo(
                    "멜버른",
                    "호주",
                    -37.6690,
                    144.8410,
                    "Australia/Melbourne"
            )),

            Map.entry("BNE", new AirportInfo(
                    "브리즈번",
                    "호주",
                    -27.3842,
                    153.1175,
                    "Australia/Brisbane"
            )),

            // 중동
            Map.entry("DXB", new AirportInfo(
                    "두바이",
                    "아랍에미리트",
                    25.2532,
                    55.3657,
                    "Asia/Dubai"
            )),

            // 유럽
            Map.entry("CDG", new AirportInfo(
                    "파리",
                    "프랑스",
                    49.0097,
                    2.5479,
                    "Europe/Paris"
            )),

            Map.entry("LHR", new AirportInfo(
                    "런던",
                    "영국",
                    51.4700,
                    -0.4543,
                    "Europe/London"
            )),

            Map.entry("FCO", new AirportInfo(
                    "로마",
                    "이탈리아",
                    41.8003,
                    12.2389,
                    "Europe/Rome"
            )),

            // 미국
            Map.entry("LAX", new AirportInfo(
                    "로스앤젤레스",
                    "미국",
                    33.9416,
                    -118.4085,
                    "America/Los_Angeles"
            )),

            Map.entry("JFK", new AirportInfo(
                    "뉴욕",
                    "미국",
                    40.6413,
                    -73.7781,
                    "America/New_York"
            )),

            Map.entry("HNL", new AirportInfo(
                    "호놀룰루",
                    "미국",
                    21.3187,
                    -157.9225,
                    "Pacific/Honolulu"
            )),

            // 캐나다
            Map.entry("YVR", new AirportInfo(
                    "밴쿠버",
                    "캐나다",
                    49.1967,
                    -123.1815,
                    "America/Vancouver"
            ))
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