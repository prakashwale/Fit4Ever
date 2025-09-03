package com.example.fit4ever.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateFoodLogRequest {
    @NotBlank
    // ISO date as String; parsed in service
    private String date;

    @NotBlank
    private String mealType; // BREAKFAST/LUNCH/DINNER/SNACK

    @NotBlank
    @Size(min = 2, max = 100)
    private String itemName;

    @Min(0)
    private Integer calories;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double protein;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double carbs;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double fat;
}


