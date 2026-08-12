package com.hackthon.hackathon.util;

public record AirportInfo(
        String city,
        String country,
        double latitude,
        double longitude,
        String timezone
) {
}
