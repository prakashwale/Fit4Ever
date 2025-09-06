package com.example.fit4ever.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkoutDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExerciseInput {
        private String name;
        private Integer setsCount;
        private Integer repsPerSet; // Keep for backward compatibility
        private Double weight; // Keep for backward compatibility
        
        // Range support
        private Integer minReps;
        private Integer maxReps;
        private Double minWeight;
        private Double maxWeight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateWorkoutRequest {
        private String title;
        private String notes;
        private LocalDate date;
        private List<ExerciseInput> exercises;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateWorkoutRequest {
        private String title;
        private String notes;
        private LocalDate date;
        private List<ExerciseInput> exercises;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExerciseResponse {
        private Long id;
        private String name;
        private Integer setsCount;
        private Integer repsPerSet; // Keep for backward compatibility
        private Double weight; // Keep for backward compatibility
        
        // Range support
        private Integer minReps;
        private Integer maxReps;
        private Double minWeight;
        private Double maxWeight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkoutSummary {
        private Long id;
        private String title;
        private String notes;
        private LocalDate date;
        private List<ExerciseResponse> exercises;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkoutDetail {
        private Long id;
        private String title;
        private String notes;
        private LocalDate date;
        private List<ExerciseResponse> exercises;
    }
}


