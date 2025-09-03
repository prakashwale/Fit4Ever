package com.example.fit4ever.repository;

import com.example.fit4ever.model.Workout;
import com.example.fit4ever.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    List<Workout> findByUserOrderByDateDesc(User user);
    Optional<Workout> findByIdAndUser(Long id, User user);
    void deleteByIdAndUser(Long id, User user);
}


