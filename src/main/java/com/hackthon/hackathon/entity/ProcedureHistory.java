package com.hackthon.hackathon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="procedure_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcedureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="procedure_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name; // 시술명

    @Column(nullable = false)
    private boolean isRecentOneMonth; // 최근 한 달 내 여부

    @Builder
    public ProcedureHistory(User user, String name, boolean isRecentOneMonth) {
        this.user = user;
        this.name = name;
        this.isRecentOneMonth = isRecentOneMonth;
    }
}