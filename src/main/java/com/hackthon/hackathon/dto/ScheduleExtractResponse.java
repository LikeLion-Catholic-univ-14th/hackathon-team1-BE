package com.hackthon.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleExtractResponse {
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private String departureTime;
    private String arrivalTime;//날짜 파싱 문제 줄이려고 나중에 일정 저장할 때 localdatetime 변환
    private Boolean isQuickTurn;
}
