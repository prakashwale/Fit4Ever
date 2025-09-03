package com.example.fit4ever.repository;

import com.example.fit4ever.model.FoodLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {
    List<FoodLog> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);
    List<FoodLog> findByUserIdAndDate(Long userId, LocalDate date);
}


