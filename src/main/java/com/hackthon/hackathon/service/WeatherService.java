package com.hackthon.hackathon.service;

import com.hackthon.hackathon.dto.WeatherResponse;
import com.hackthon.hackathon.util.AirportInfo;
import com.hackthon.hackathon.util.AirportLocationMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    public WeatherResponse getWeather(String airportCode) {

        AirportInfo airport =
                AirportLocationMapper.getAirportInfo(airportCode);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("latitude", airport.latitude())
                        .queryParam("longitude", airport.longitude())
                        .queryParam(
                                "hourly",
                                "temperature_2m,weather_code,uv_index"
                        )
                        .queryParam(
                                "daily",
                                "sunrise,sunset"
                        )
                        .queryParam(
                                "timezone",
                                airport.timezone()
                        )
                        .queryParam(
                                "past_days",
                                31
                        )
                        .queryParam(
                                "forecast_days",
                                16
                        )
                        .build())
                .retrieve()
                .body(WeatherResponse.class);
    }

    public WeatherResponse getSeoulWeather() {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam(
                                "latitude",
                                37.5665
                        )
                        .queryParam(
                                "longitude",
                                126.9780
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
                                "Asia/Seoul"
                        )
                        .build()
                )
                .retrieve()
                .body(WeatherResponse.class);
    }
}