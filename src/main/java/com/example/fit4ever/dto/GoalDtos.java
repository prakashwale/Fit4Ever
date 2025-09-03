package com.example.fit4ever.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

public class GoalDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateGoalRequest {
        @NotBlank
        private String type;             // validate in service
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        private Double targetValue;
        @NotBlank
        private String startDate;        // "YYYY-MM-DD"
        @NotBlank
        private String endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateGoalRequest {
        private String type;
        private Double targetValue;
        private String startDate;
        private String endDate;
        private String status;           // ACTIVE/COMPLETED/CANCELLED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalResponse {
        private Long id;
        private String type;
        private Double targetValue;
        private String startDate;
        private String endDate;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalProgressResponse {
        private Long id;
        private String type;
        private Double targetValue;
        private Double progress;   // 0..1
        private String status;
    }
}
