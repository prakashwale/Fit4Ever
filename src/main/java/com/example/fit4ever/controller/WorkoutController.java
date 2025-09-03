package com.example.fit4ever.controller;

import com.example.fit4ever.dto.WorkoutDtos.CreateWorkoutRequest;
import com.example.fit4ever.dto.WorkoutDtos.UpdateWorkoutRequest;
import com.example.fit4ever.dto.WorkoutDtos.WorkoutDetail;
import com.example.fit4ever.dto.WorkoutDtos.WorkoutSummary;
import com.example.fit4ever.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {
    private final WorkoutService workoutService;

    @PostMapping
    public WorkoutDetail create(Authentication authentication, @RequestBody CreateWorkoutRequest request) {
        return workoutService.create(authentication, request);
    }

    @GetMapping
    public List<WorkoutSummary> list(Authentication authentication) {
        return workoutService.list(authentication);
    }

    @GetMapping("/{id}")
    public WorkoutDetail get(Authentication authentication, @PathVariable Long id) {
        return workoutService.get(authentication, id);
    }

    @PutMapping("/{id}")
    public WorkoutDetail update(Authentication authentication, @PathVariable Long id, @RequestBody UpdateWorkoutRequest request) {
        return workoutService.update(authentication, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable Long id) {
        workoutService.delete(authentication, id);
    }
}


