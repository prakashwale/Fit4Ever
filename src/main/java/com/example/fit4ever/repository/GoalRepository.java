package com.example.fit4ever.repository;

import com.example.fit4ever.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
  List<Goal> findByUserIdOrderByStartDateDesc(Long userId);
}
