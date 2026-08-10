package com.hackthon.hackathon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "solution",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"exposure_id", "sunscreen_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solution_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exposure_id", nullable = false)
    private ExposureRecord exposureRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sunscreen_id", nullable = false)
    private Sunscreen sunscreen;

    @Column(name = "comment", columnDefinition = "JSON", nullable = false)
    private String comment;

    @Column(name = "is_default", nullable = false)
    private boolean defaultSolution;

    @Column(name = "is_applied", nullable = false)
    private boolean applied = false;

    @Column(name = "effective_spf", nullable = false)
    private Double effectiveSpf;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
