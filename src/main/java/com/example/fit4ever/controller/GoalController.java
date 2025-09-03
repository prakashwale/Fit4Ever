package com.example.fit4ever.controller;

import com.example.fit4ever.dto.GoalDtos.*;
import com.example.fit4ever.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController 
@RequestMapping("/api/goals") 
@RequiredArgsConstructor
@Validated
@Tag(name = "Goals", description = "Goal setting and tracking")
public class GoalController {
    private final GoalService goalService;

    @Operation(summary = "Create a new goal")
    @PostMapping
    public GoalResponse create(@Valid @RequestBody CreateGoalRequest r, Authentication auth) {
        return goalService.create(r, auth.getName());
    }

    @Operation(summary = "List all goals for the authenticated user")
    @GetMapping
    public List<GoalResponse> list(Authentication auth) {
        return goalService.list(auth.getName());
    }

    @Operation(summary = "Update an existing goal")
    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGoalRequest r, Authentication auth) {
        return goalService.update(id, r, auth.getName());
    }

    @Operation(summary = "Get progress for a specific goal")
    @GetMapping("/{id}/progress")
    public GoalProgressResponse progress(@PathVariable Long id, Authentication auth) {
        return goalService.progress(id, auth.getName());
    }
}
