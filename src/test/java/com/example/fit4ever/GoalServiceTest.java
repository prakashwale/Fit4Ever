package com.example.fit4ever;

import com.example.fit4ever.dto.GoalDtos.*;
import com.example.fit4ever.model.Goal;
import com.example.fit4ever.model.User;
import com.example.fit4ever.repository.GoalRepository;
import com.example.fit4ever.repository.UserRepository;
import com.example.fit4ever.service.GoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GoalServiceTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateAndListGoal() {
        // Create a test user
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role("USER")
                .build();
        user = userRepository.save(user);

        // Create a goal request
        CreateGoalRequest request = CreateGoalRequest.builder()
                .type("WEIGHT")
                .targetValue(70.0)
                .startDate("2025-01-01")
                .endDate("2025-12-31")
                .build();

        // Create the goal
        GoalResponse response = goalService.create(request, user.getEmail());
        
        assertNotNull(response);
        assertEquals("WEIGHT", response.getType());
        assertEquals(70.0, response.getTargetValue());
        assertEquals("ACTIVE", response.getStatus());

        // List goals
        List<GoalResponse> goals = goalService.list(user.getEmail());
        assertEquals(1, goals.size());
        assertEquals("WEIGHT", goals.get(0).getType());
    }

    @Test
    public void testGoalProgress() {
        // Create a test user
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role("USER")
                .build();
        user = userRepository.save(user);

        // Create a goal
        Goal goal = Goal.builder()
                .user(user)
                .type("WORKOUTS_PER_WEEK")
                .targetValue(3.0)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status("ACTIVE")
                .build();
        goal = goalRepository.save(goal);

        // Get progress
        GoalProgressResponse progress = goalService.progress(goal.getId(), user.getEmail());
        
        assertNotNull(progress);
        assertEquals("WORKOUTS_PER_WEEK", progress.getType());
        assertEquals(3.0, progress.getTargetValue());
        assertEquals(0.6, progress.getProgress()); // Demo value from service
    }
}
