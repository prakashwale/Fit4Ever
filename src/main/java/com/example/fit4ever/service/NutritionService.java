package com.example.fit4ever.service;

import com.example.fit4ever.dto.CreateFoodLogRequest;
import com.example.fit4ever.dto.FoodLogResponse;
import com.example.fit4ever.dto.NutritionSummaryResponse;
import com.example.fit4ever.model.FoodLog;
import com.example.fit4ever.model.User;
import com.example.fit4ever.repository.FoodLogRepository;
import com.example.fit4ever.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NutritionService {
    private final UserRepository userRepository;
    private final FoodLogRepository foodLogRepository;

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private static boolean isValidMealType(String mealType) {
        if (mealType == null) return false;
        String mt = mealType.toUpperCase(Locale.ROOT).trim();
        return mt.equals("BREAKFAST") || mt.equals("LUNCH") || mt.equals("DINNER") || mt.equals("SNACK");
    }

    private static FoodLogResponse toResponse(FoodLog f) {
        return FoodLogResponse.builder()
                .id(f.getId())
                .date(f.getDate().toString())
                .mealType(f.getMealType())
                .itemName(f.getItemName())
                .calories(f.getCalories())
                .protein(f.getProtein())
                .carbs(f.getCarbs())
                .fat(f.getFat())
                .build();
    }

    @Transactional
    public FoodLogResponse createFoodLog(CreateFoodLogRequest req, String userEmail) {
        if (!isValidMealType(req.getMealType())) {
            throw new IllegalArgumentException("mealType must be one of BREAKFAST/LUNCH/DINNER/SNACK");
        }
        User user = requireUser(userEmail);
        LocalDate date = LocalDate.parse(req.getDate());

        FoodLog log = FoodLog.builder()
                .date(date)
                .mealType(req.getMealType().toUpperCase(Locale.ROOT))
                .itemName(req.getItemName())
                .calories(req.getCalories())
                .protein(req.getProtein())
                .carbs(req.getCarbs())
                .fat(req.getFat())
                .user(user)
                .build();
        return toResponse(foodLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<FoodLogResponse> listByDate(String date, String userEmail) {
        User user = requireUser(userEmail);
        LocalDate d = LocalDate.parse(date);
        return foodLogRepository.findByUserIdAndDate(user.getId(), d).stream()
                .sorted(Comparator.comparing(FoodLog::getMealType))
                .map(NutritionService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(Long id, String userEmail) {
        User user = requireUser(userEmail);
        FoodLog log = foodLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FoodLog not found"));
        if (!log.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Forbidden: cannot delete others' logs");
        }
        foodLogRepository.delete(log);
    }

    @Transactional(readOnly = true)
    public NutritionSummaryResponse summary(String from, String to, String userEmail) {
        User user = requireUser(userEmail);
        LocalDate toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        LocalDate fromDate = (from == null || from.isBlank()) ? toDate.minusDays(6) : LocalDate.parse(from);

        List<FoodLog> logs = foodLogRepository.findByUserIdAndDateBetween(user.getId(), fromDate, toDate);

        Map<LocalDate, List<FoodLog>> byDate = logs.stream().collect(Collectors.groupingBy(FoodLog::getDate));

        int totalCalories = logs.stream().mapToInt(FoodLog::getCalories).sum();
        double totalProtein = logs.stream().mapToDouble(FoodLog::getProtein).sum();
        double totalCarbs = logs.stream().mapToDouble(FoodLog::getCarbs).sum();
        double totalFat = logs.stream().mapToDouble(FoodLog::getFat).sum();

        List<NutritionSummaryResponse.ByDay> byDay = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    int c = e.getValue().stream().mapToInt(FoodLog::getCalories).sum();
                    double p = e.getValue().stream().mapToDouble(FoodLog::getProtein).sum();
                    double cb = e.getValue().stream().mapToDouble(FoodLog::getCarbs).sum();
                    double f = e.getValue().stream().mapToDouble(FoodLog::getFat).sum();
                    return NutritionSummaryResponse.ByDay.builder()
                            .date(e.getKey().toString())
                            .calories(c)
                            .protein(p)
                            .carbs(cb)
                            .fat(f)
                            .build();
                })
                .collect(Collectors.toList());

        return NutritionSummaryResponse.builder()
                .from(fromDate.toString())
                .to(toDate.toString())
                .totals(NutritionSummaryResponse.Totals.builder()
                        .calories(totalCalories)
                        .protein(totalProtein)
                        .carbs(totalCarbs)
                        .fat(totalFat)
                        .build())
                .byDay(byDay)
                .build();
    }
}


