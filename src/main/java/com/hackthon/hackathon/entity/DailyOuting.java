package com.hackthon.hackathon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_outing",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_outing_user_date",
                        columnNames = {"user_id", "outing_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyOuting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_outing_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "outing_date", nullable = false)
    private LocalDate date;

    @Column(name = "is_outing", nullable = false)
    private boolean outing;

    @Builder
    public DailyOuting(
            User user,
            LocalDate date,
            boolean outing
    ) {
        this.user = user;
        this.date = date;
        this.outing = outing;
    }

    public void updateOuting(boolean outing) {
        this.outing = outing;
    }
}