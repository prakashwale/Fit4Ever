package com.example.fit4ever.service;

import com.example.fit4ever.dto.WorkoutDtos.*;
import com.example.fit4ever.model.Exercise;
import com.example.fit4ever.model.User;
import com.example.fit4ever.model.Workout;
import com.example.fit4ever.repository.UserRepository;
import com.example.fit4ever.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @Transactional
    public WorkoutDetail create(Authentication authentication, CreateWorkoutRequest request) {
        User user = getCurrentUser(authentication);
        Workout workout = Workout.builder()
                .user(user)
                .title(request.getTitle())
                .notes(request.getNotes())
                .date(request.getDate())
                .build();

        if (request.getExercises() != null) {
            for (var ex : request.getExercises()) {
                Exercise e = Exercise.builder()
                        .workout(workout)
                        .name(ex.getName())
                        .setsCount(ex.getSetsCount())
                        .repsPerSet(ex.getRepsPerSet())
                        .weight(ex.getWeight())
                        .build();
                workout.getExercises().add(e);
            }
        }

        Workout saved = workoutRepository.save(workout);
        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkoutSummary> list(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return workoutRepository.findByUserOrderByDateDesc(user).stream()
                .map(w -> WorkoutSummary.builder()
                        .id(w.getId())
                        .title(w.getTitle())
                        .date(w.getDate())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkoutDetail get(Authentication authentication, Long id) {
        User user = getCurrentUser(authentication);
        Workout workout = workoutRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));
        return toDetail(workout);
    }

    @Transactional
    public WorkoutDetail update(Authentication authentication, Long id, UpdateWorkoutRequest request) {
        User user = getCurrentUser(authentication);
        Workout workout = workoutRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (request.getTitle() != null) workout.setTitle(request.getTitle());
        if (request.getNotes() != null) workout.setNotes(request.getNotes());
        if (request.getDate() != null) workout.setDate(request.getDate());

        // Replace exercises if provided
        if (request.getExercises() != null) {
            workout.getExercises().clear();
            for (var ex : request.getExercises()) {
                Exercise e = Exercise.builder()
                        .workout(workout)
                        .name(ex.getName())
                        .setsCount(ex.getSetsCount())
                        .repsPerSet(ex.getRepsPerSet())
                        .weight(ex.getWeight())
                        .build();
                workout.getExercises().add(e);
            }
        }

        Workout saved = workoutRepository.save(workout);
        return toDetail(saved);
    }

    @Transactional
    public void delete(Authentication authentication, Long id) {
        User user = getCurrentUser(authentication);
        Workout workout = workoutRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));
        workoutRepository.delete(workout);
    }

    private static WorkoutDetail toDetail(Workout w) {
        return WorkoutDetail.builder()
                .id(w.getId())
                .title(w.getTitle())
                .notes(w.getNotes())
                .date(w.getDate())
                .exercises(w.getExercises().stream().map(e -> ExerciseResponse.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .setsCount(e.getSetsCount())
                        .repsPerSet(e.getRepsPerSet())
                        .weight(e.getWeight())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}


