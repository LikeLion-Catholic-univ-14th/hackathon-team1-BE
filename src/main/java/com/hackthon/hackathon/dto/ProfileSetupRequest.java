package com.hackthon.hackathon.dto;

import com.hackthon.hackathon.enums.BaseAirport;
import com.hackthon.hackathon.enums.SkinConcern;
import com.hackthon.hackathon.enums.SkinType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
public class ProfileSetupRequest {
    private String name;
    private BaseAirport baseAirport;
    private Set<SkinType> skinTypes = new HashSet<>();
    private Set<SkinConcern> skinConcerns = new HashSet<>(); //나중에 JSON 변환 처리..

    private ProcedureDto procedure;

    @Getter
    @NoArgsConstructor
    public static  class ProcedureDto {
        private boolean hasProcedureHistory;
        private String procedureDetails;
        private Boolean procedureWithinOneMonth;
    }
}
