package com.example.fit4ever.controller;

import com.example.fit4ever.dto.CreateFoodLogRequest;
import com.example.fit4ever.dto.FoodLogResponse;
import com.example.fit4ever.dto.NutritionSummaryResponse;
import com.example.fit4ever.service.NutritionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
@Validated
@Tag(name = "Nutrition", description = "Nutrition logs and summaries")
public class NutritionController {
    private final NutritionService nutritionService;

    @Operation(summary = "Create a food log")
    @ApiResponse(responseCode = "200", description = "Created log",
            content = @Content(schema = @Schema(implementation = FoodLogResponse.class)))
    @PostMapping("/logs")
    public FoodLogResponse createFoodLog(Authentication auth, @Valid @org.springframework.web.bind.annotation.RequestBody CreateFoodLogRequest request) {
        return nutritionService.createFoodLog(request, auth.getName());
    }

    @Operation(summary = "List logs by date")
    @GetMapping("/logs")
    public List<FoodLogResponse> listByDate(Authentication auth, @RequestParam("date") String date) {
        return nutritionService.listByDate(date, auth.getName());
    }

    @Operation(summary = "Delete a log by id")
    @DeleteMapping("/logs/{id}")
    public void deleteById(Authentication auth, @PathVariable Long id) {
        nutritionService.deleteById(id, auth.getName());
    }

    @Operation(summary = "Summary of nutrition over a date range")
    @GetMapping("/summary")
    public NutritionSummaryResponse summary(Authentication auth,
                                            @RequestParam(value = "from", required = false) String from,
                                            @RequestParam(value = "to", required = false) String to) {
        return nutritionService.summary(from, to, auth.getName());
    }
}


