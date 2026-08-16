package com.hackthon.hackathon.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbTimeZoneCheckRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {

        String sessionTimeZone =
                jdbcTemplate.queryForObject(
                        "SELECT @@session.time_zone",
                        String.class
                );

        String globalTimeZone =
                jdbcTemplate.queryForObject(
                        "SELECT @@global.time_zone",
                        String.class
                );

        System.out.println(
                "===== DB SESSION TIMEZONE: "
                        + sessionTimeZone
        );

        System.out.println(
                "===== DB GLOBAL TIMEZONE: "
                        + globalTimeZone
        );
    }
}