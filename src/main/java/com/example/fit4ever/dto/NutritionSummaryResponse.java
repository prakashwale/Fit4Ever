package com.example.fit4ever.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionSummaryResponse {
    private String from;
    private String to;
    private Totals totals;
    private List<ByDay> byDay;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Totals {
        private Integer calories;
        private Double protein;
        private Double carbs;
        private Double fat;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ByDay {
        private String date;
        private Integer calories;
        private Double protein;
        private Double carbs;
        private Double fat;
    }
}


