package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.util.AirportInfo;
import com.hackthon.hackathon.util.AirportLocationMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Service
public class WeatherService {

    private final RestClient forecastClient;
    private final RestClient historicalForecastClient;

    public WeatherService() {

        this.forecastClient =
                RestClient.builder()
                        .baseUrl(
                                "https://api.open-meteo.com/v1"
                        )
                        .build();

        this.historicalForecastClient =
                RestClient.builder()
                        .baseUrl(
                                "https://historical-forecast-api.open-meteo.com/v1"
                        )
                        .build();
    }

    // /today 등 현재 날씨/예보 조회
    public WeatherResponse getWeather(
            String airportCode
    ) {

        AirportInfo airport =
                AirportLocationMapper.getAirportInfo(
                        airportCode
                );

        return forecastClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/forecast")

                                .queryParam(
                                        "latitude",
                                        airport.latitude()
                                )

                                .queryParam(
                                        "longitude",
                                        airport.longitude()
                                )

                                .queryParam(
                                        "hourly",
                                        "uv_index,temperature_2m,weather_code"
                                )

                                .queryParam(
                                        "daily",
                                        "sunrise,sunset"
                                )

                                .queryParam(
                                        "timezone",
                                        airport.timezone()
                                )

                                .build()
                )
                .retrieve()
                .body(
                        WeatherResponse.class
                );
    }

    // 날짜 상세 조회
    public WeatherResponse getWeather(
            String airportCode,
            LocalDate date
    ) {

        AirportInfo airport =
                AirportLocationMapper.getAirportInfo(
                        airportCode
                );

        LocalDate today =
                LocalDate.now();

        /*
         * 과거 날짜:
         * Historical Forecast API 사용
         */
        if (date.isBefore(today)) {

            return historicalForecastClient.get()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .path("/forecast")

                                    .queryParam(
                                            "latitude",
                                            airport.latitude()
                                    )

                                    .queryParam(
                                            "longitude",
                                            airport.longitude()
                                    )

                                    .queryParam(
                                            "start_date",
                                            date
                                    )

                                    .queryParam(
                                            "end_date",
                                            date
                                    )

                                    .queryParam(
                                            "hourly",
                                            "uv_index,temperature_2m,weather_code"
                                    )

                                    .queryParam(
                                            "daily",
                                            "sunrise,sunset"
                                    )

                                    .queryParam(
                                            "timezone",
                                            airport.timezone()
                                    )

                                    .build()
                    )
                    .retrieve()
                    .body(
                            WeatherResponse.class
                    );
        }

        /*
         * 오늘/미래 날짜:
         * 기존 Forecast API 사용
         */
        return forecastClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/forecast")

                                .queryParam(
                                        "latitude",
                                        airport.latitude()
                                )

                                .queryParam(
                                        "longitude",
                                        airport.longitude()
                                )

                                .queryParam(
                                        "start_date",
                                        date
                                )

                                .queryParam(
                                        "end_date",
                                        date
                                )

                                .queryParam(
                                        "hourly",
                                        "uv_index,temperature_2m,weather_code"
                                )

                                .queryParam(
                                        "daily",
                                        "sunrise,sunset"
                                )

                                .queryParam(
                                        "timezone",
                                        airport.timezone()
                                )

                                .build()
                )
                .retrieve()
                .body(
                        WeatherResponse.class
                );
    }
}