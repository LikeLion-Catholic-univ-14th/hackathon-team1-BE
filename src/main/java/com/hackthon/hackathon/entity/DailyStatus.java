package com.hackthon.hackathon.entity;

import com.hackthon.hackathon.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_status",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"user_id", "date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean outing;

    @Builder
    public DailyStatus(
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