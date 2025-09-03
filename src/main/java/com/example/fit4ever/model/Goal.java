package com.example.fit4ever.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity 
@Table(name="goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(nullable=false) 
    private String type; // WEIGHT | WORKOUTS_PER_WEEK | CALORIES
    
    @Column(nullable=false) 
    private Double targetValue;
    
    @Column(nullable=false) 
    private LocalDate startDate;
    
    @Column(nullable=false) 
    private LocalDate endDate;
    
    @Column(nullable=false) 
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE | COMPLETED | CANCELLED
} 