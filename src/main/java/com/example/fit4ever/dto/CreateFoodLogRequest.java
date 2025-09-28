package com.example.fit4ever.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateFoodLogRequest {
    
    @NotBlank(message = "Date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in YYYY-MM-DD format")
    private String date;

    @NotBlank(message = "Meal type is required")
    @Pattern(regexp = "^(BREAKFAST|LUNCH|DINNER|SNACK)$", message = "Meal type must be BREAKFAST, LUNCH, DINNER, or SNACK")
    private String mealType;

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 100, message = "Item name must be between 2 and 100 characters")
    private String itemName;

    @NotNull(message = "Calories is required")
    @Min(value = 0, message = "Calories must be non-negative")
    @Max(value = 10000, message = "Calories must not exceed 10,000")
    private Integer calories;

    @NotNull(message = "Protein is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Protein must be non-negative")
    @DecimalMax(value = "1000.0", message = "Protein must not exceed 1000g")
    private Double protein;

    @NotNull(message = "Carbs is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Carbs must be non-negative")
    @DecimalMax(value = "1000.0", message = "Carbs must not exceed 1000g")
    private Double carbs;

    @NotNull(message = "Fat is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fat must be non-negative")
    @DecimalMax(value = "1000.0", message = "Fat must not exceed 1000g")
    private Double fat;
}


