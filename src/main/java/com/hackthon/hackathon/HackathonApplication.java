package com.hackthon.hackathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class HackathonApplication {

	public static void main(String[] args) {

		System.out.println(
				"===== JVM ZoneId: "
						+ ZoneId.systemDefault()
		);

		System.out.println(
				"===== JVM TimeZone: "
						+ TimeZone.getDefault().getID()
		);

		SpringApplication.run(
				HackathonApplication.class,
				args
		);
	}
}