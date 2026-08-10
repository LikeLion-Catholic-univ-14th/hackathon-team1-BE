package com.hackthon.hackathon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "monthly_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"user_id", "report_year", "report_month"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "report_year", nullable = false)
    private Integer reportYear;

    @Column(name = "report_month", nullable = false)
    private Integer reportMonth;

    @Column(name = "actual_outing_hours", nullable = false)
    private Integer actualOutingHours;

    @Column(name = "equivalent_days_in_seoul", nullable = false)
    private Double equivalentDaysInSeoul;

    @Column(name = "route_ranking", columnDefinition = "JSON", nullable = false)
    private String routeRanking;

    @Column(columnDefinition = "JSON", nullable = false)
    private String trend;

    @Column(columnDefinition = "JSON", nullable = false)
    private String analysis;

    @Column(name = "next_month_forecast", columnDefinition = "JSON", nullable = false)
    private String nextMonthForecast;

    @Column(columnDefinition = "JSON", nullable = false)
    private String clinic;

    @Column(name = "daily_exposure", columnDefinition = "JSON", nullable = false)
    private String dailyExposure;

    @Column(name = "finalized_at", nullable = false)
    private LocalDateTime finalizedAt;
}
