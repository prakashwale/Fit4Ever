package com.example.fit4ever.service;

import com.example.fit4ever.dto.GoalDtos.*;
import com.example.fit4ever.model.Goal;
import com.example.fit4ever.model.User;
import com.example.fit4ever.repository.GoalRepository;
import com.example.fit4ever.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service 
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goalRepo;
    private final UserRepository userRepo;

    private User userByEmail(String email) {
        return userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private static LocalDate d(String s){ 
        return LocalDate.parse(s); 
    }

    @Transactional
    public GoalResponse create(CreateGoalRequest r, String email) {
        validateType(r.getType());
        var u = userByEmail(email);
        var g = Goal.builder()
                .user(u)
                .type(r.getType())
                .targetValue(r.getTargetValue())
                .startDate(d(r.getStartDate()))
                .endDate(d(r.getEndDate()))
                .status("ACTIVE")
                .build();
        g = goalRepo.save(g);
        return toResp(g);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(String email) {
        var u = userByEmail(email);
        return goalRepo.findByUserIdOrderByStartDateDesc(u.getId())
                .stream().map(this::toResp).toList();
    }

    @Transactional
    public GoalResponse update(Long id, UpdateGoalRequest r, String email) {
        var u = userByEmail(email);
        var g = goalRepo.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!g.getUser().getId().equals(u.getId())) throw new RuntimeException("Forbidden");
        
        if (r.getType() != null) { 
            validateType(r.getType()); 
            g.setType(r.getType()); 
        }
        if (r.getTargetValue() != null) g.setTargetValue(r.getTargetValue());
        if (r.getStartDate() != null) g.setStartDate(d(r.getStartDate()));
        if (r.getEndDate() != null) g.setEndDate(d(r.getEndDate()));
        if (r.getStatus() != null) g.setStatus(r.getStatus());
        
        g = goalRepo.save(g);
        return toResp(g);
    }

    @Transactional(readOnly = true)
    public GoalProgressResponse progress(Long id, String email) {
        var u = userByEmail(email);
        var g = goalRepo.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!g.getUser().getId().equals(u.getId())) throw new RuntimeException("Forbidden");
        
        // Simple demo progress calc (replace with real aggregation later):
        double progress = switch (g.getType()) {
            case "WEIGHT" -> 0.4;            // e.g., from latest weigh-ins
            case "WORKOUTS_PER_WEEK" -> 0.6; // e.g., count workouts this week / target
            case "CALORIES" -> 0.5;          // e.g., compare avg calories to target
            default -> 0.0;
        };
        
        return GoalProgressResponse.builder()
                .id(g.getId())
                .type(g.getType())
                .targetValue(g.getTargetValue())
                .progress(progress)
                .status(g.getStatus())
                .build();
    }

    private void validateType(String t) {
        if (!( "WEIGHT".equals(t) || "WORKOUTS_PER_WEEK".equals(t) || "CALORIES".equals(t) ))
            throw new IllegalArgumentException("type must be one of: WEIGHT | WORKOUTS_PER_WEEK | CALORIES");
    }

    private GoalResponse toResp(Goal g) {
        return GoalResponse.builder()
                .id(g.getId())
                .type(g.getType())
                .targetValue(g.getTargetValue())
                .startDate(g.getStartDate().toString())
                .endDate(g.getEndDate().toString())
                .status(g.getStatus())
                .build();
    }
}
