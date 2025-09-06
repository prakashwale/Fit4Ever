package com.example.fit4ever.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "exercises")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(nullable = false)
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


