package com.hackthon.hackathon.entity;

import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.enums.SkinConcern;
import com.hackthon.hackathon.enums.SkinType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skin_types", columnDefinition = "json")
    private Set<SkinType> skinTypes = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skin_concerns", columnDefinition = "json")
    private Set<SkinConcern> skinConcerns = new HashSet<>(); //나중에 JSON 변환 처리..

    @Enumerated(EnumType.STRING)
    @Column(name = "base_airport", nullable = false)
    private BaseAirport baseAirport;

    @Column(name = "has_procedure_history", nullable = false)
    private boolean hasProcedureHistory;

    @Column(name = "procedure_details", length = 100)
    private String procedureDetails;

    @Column(name = "procedure_within_one_month")
    private Boolean procedureWithinOneMonth;

    public void setupProfile(String name, BaseAirport baseAirport, Set<SkinType> skinTypes,
                             Set<SkinConcern> skinConcerns, boolean hasProcedureHistory,
                             String procedureDetails, Boolean procedureWithinOneMonth) {
        this.name = name;
        this.baseAirport = baseAirport;
        this.skinTypes = skinTypes;
        this.skinConcerns = skinConcerns;
        this.hasProcedureHistory = hasProcedureHistory;
        this.procedureDetails = procedureDetails;
        this.procedureWithinOneMonth = procedureWithinOneMonth;
    }
}
