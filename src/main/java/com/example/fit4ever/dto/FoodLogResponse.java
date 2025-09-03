package com.example.fit4ever.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodLogResponse {
    private Long id;
    private String date;
    private String mealType;
    private String itemName;
    private Integer calories;
    private Double protein;
    private Double carbs;
    private Double fat;
}


