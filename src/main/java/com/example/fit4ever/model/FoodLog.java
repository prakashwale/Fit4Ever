package com.example.fit4ever.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "food_logs", indexes = {
        @Index(name = "idx_foodlog_user_date", columnList = "user_id,date")
})
public class FoodLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 20)
    private String mealType; // Validated to be one of BREAKFAST/LUNCH/DINNER/SNACK

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String itemName;

    @Min(0)
    @Column(nullable = false)
    private Integer calories;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false)
    private Double protein;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false)
    private Double carbs;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false)
    private Double fat;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}


