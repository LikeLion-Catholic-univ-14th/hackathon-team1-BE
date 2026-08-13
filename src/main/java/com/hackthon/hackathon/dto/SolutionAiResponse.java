package com.hackthon.hackathon.dto;

import java.util.List;

public record SolutionAiResponse(
        Long sunscreenId,
        String message,
        List<Solution> solutions
) {

    public record Solution(
            String phase,
            String title,
            String description
    ) {
    }
}