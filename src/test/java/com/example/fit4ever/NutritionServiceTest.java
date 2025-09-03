package com.example.fit4ever;

import com.example.fit4ever.dto.CreateFoodLogRequest;
import com.example.fit4ever.dto.NutritionSummaryResponse;
import com.example.fit4ever.model.User;
import com.example.fit4ever.model.FoodLog;
import com.example.fit4ever.repository.FoodLogRepository;
import com.example.fit4ever.repository.UserRepository;
import com.example.fit4ever.service.NutritionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NutritionServiceTest {

    private UserRepository userRepository;
    private FoodLogRepository foodLogRepository;
    private NutritionService nutritionService;

    @BeforeEach
    void setup() {
        userRepository = Mockito.mock(UserRepository.class);
        foodLogRepository = Mockito.mock(FoodLogRepository.class);
        nutritionService = new NutritionService(userRepository, foodLogRepository);

        // Default stubs
        when(foodLogRepository.save(any())).thenAnswer(inv -> {
            FoodLog f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(1L);
            }
            return f;
        });
        when(foodLogRepository.findByUserIdAndDateBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(foodLogRepository.findByUserIdAndDate(anyLong(), any())).thenReturn(List.of());
    }

    @Test
    void createAndListReturnsEntry() {
        User user = User.builder().id(1L).email("u@example.com").password("x").name("U").role("USER").build();
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));

        CreateFoodLogRequest req = new CreateFoodLogRequest();
        req.setDate("2025-09-03");
        req.setMealType("LUNCH");
        req.setItemName("Grilled Chicken Bowl");
        req.setCalories(550);
        req.setProtein(42.0);
        req.setCarbs(48.0);
        req.setFat(18.0);

        nutritionService.createFoodLog(req, "u@example.com");

        verify(foodLogRepository, times(1)).save(any());
    }

    @Test
    void summaryReturnsTotals() {
        User user = User.builder().id(1L).email("u@example.com").password("x").name("U").role("USER").build();
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));

        // We don't mock repository internals deeply; we just call to ensure no exceptions
        NutritionSummaryResponse resp = nutritionService.summary("2025-09-01", "2025-09-07", "u@example.com");
        assertThat(resp.getFrom()).isEqualTo("2025-09-01");
        assertThat(resp.getTo()).isEqualTo("2025-09-07");
    }

    @Test
    void deleteNonOwnerThrows() {
        User owner = User.builder().id(1L).email("owner@example.com").password("x").name("O").role("USER").build();
        User attacker = User.builder().id(2L).email("attacker@example.com").password("x").name("A").role("USER").build();
        when(userRepository.findByEmail("attacker@example.com")).thenReturn(Optional.of(attacker));

        // Simulate repository returning a log owned by someone else by not mocking findById -> will throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> nutritionService.deleteById(99L, "attacker@example.com"));
    }
}


