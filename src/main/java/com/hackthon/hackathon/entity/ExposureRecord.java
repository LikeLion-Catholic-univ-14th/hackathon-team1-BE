package com.hackthon.hackathon.entity;

import com.hackthon.hackathon.enums.LocationType;
import com.hackthon.hackathon.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exposure_record")
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ExposureRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exposure_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "airport_code", nullable = false, length = 10)
    private String airportCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private LocationType locationType;

    @Column(name = "uv_index", nullable = false)
    private Integer uvIndex;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer temperature;

    @Column(name = "required_spf", nullable = false)
    private Integer requiredSpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "sunlight_start", nullable = false)
    private LocalDateTime sunlightStart;

    @Column(name = "average_uv", nullable = false)
    private Double averageUv;

    @Column(name = "sunlight_end", nullable = false)
    private LocalDateTime sunlightEnd;

    @Column(name = "sunlight_minutes", nullable = false)
    private Integer sunlightMinutes;

    @Column(name = "is_outing", nullable = false)
    private boolean isOuting = true;

    @Column(name = "estimated_exposure_score")
    private Double estimatedExposureScore;

    @Column(name = "korea_comparison", nullable = false)
    private Double koreaComparison;

    @Column(name = "weather_condition", nullable = false, length = 100)
    private String weatherCondition;
}
