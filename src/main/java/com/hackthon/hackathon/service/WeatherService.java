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
                .body(WeatherResponse.class);
    }
}
