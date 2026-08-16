package com.hackthon.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class WeatherResponse {

    private String timezone;

    private Hourly hourly;

    private Daily daily;

    @Getter
    @NoArgsConstructor
    public static class Hourly {

        private List<String> time;

        @JsonProperty("uv_index")
        private List<Double> uvIndex;

        @JsonProperty("temperature_2m")
        private List<Double> temperature;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;
    }

    @Getter
    @NoArgsConstructor
    public static class Daily {

        private List<String> sunrise;

        private List<String> sunset;
    }
}