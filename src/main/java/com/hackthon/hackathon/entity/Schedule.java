package com.hackthon.hackathon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "departure_airport", nullable = false, length = 20)
    private String departureAirport;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "arrival_airport", nullable = false, length = 20)
    private String arrivalAirport;

    @Column(name = "flight_number", nullable = false, length = 20)
    private String flightNumber;

    @Column(name = "is_quick_turn", nullable = false)
    private boolean quickTurn;

    @Column(name = "is_outing", nullable = false)
    private boolean outing = true;

    // 실제 선택한 선크림
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_sunscreen_id")
    private Sunscreen selectedSunscreen;

    // 실제 적용 여부
    @Column(name = "is_applied", nullable = false)
    private boolean applied = false;


    public static Schedule create(
            User user,
            String flightNumber,
            String departureAirport,
            String arrivalAirport,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            boolean quickTurn
    ) {

        Schedule schedule = new Schedule();

        schedule.user = user;
        schedule.flightNumber = flightNumber;
        schedule.departureAirport = departureAirport;
        schedule.arrivalAirport = arrivalAirport;
        schedule.departureTime = departureTime;
        schedule.arrivalTime = arrivalTime;
        schedule.quickTurn = quickTurn;

        return schedule;
    }

    public void updateOuting(boolean outing) {
        this.outing = outing;
    }

    public void update(
            String flightNumber,
            String departureAirport,
            String arrivalAirport,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            boolean quickTurn
    ) {
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.quickTurn = quickTurn;
    }

    public void applySunscreen(
            Sunscreen sunscreen,
            boolean applied
    ) {
        this.selectedSunscreen = sunscreen;
        this.applied = applied;
    }

    public void removeSelectedSunscreen() {
        this.selectedSunscreen = null;
        this.applied = false;
    }
}