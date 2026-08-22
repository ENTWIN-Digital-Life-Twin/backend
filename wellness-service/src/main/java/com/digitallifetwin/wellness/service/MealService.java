package com.digitallifetwin.wellness.service;

import com.digitallifetwin.wellness.dto.request.CreateMealRequest;
import com.digitallifetwin.wellness.dto.request.UpdateMealRequest;
import com.digitallifetwin.wellness.dto.response.MealResponse;
import com.digitallifetwin.wellness.dto.response.PageResponse;
import com.digitallifetwin.wellness.entity.Meal;
import com.digitallifetwin.wellness.enums.MealType;
import com.digitallifetwin.wellness.exception.MealNotFoundException;
import com.digitallifetwin.wellness.mapper.WellnessMapper;
import com.digitallifetwin.wellness.repository.MealRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;
    private final WellnessMapper wellnessMapper;

    @Transactional
    public MealResponse create(UUID userId, CreateMealRequest request) {
        validateNutrition(request.totalCalories(), request.proteinGrams(), request.carbohydrateGrams(),
                request.fatGrams(), request.fibreGrams());

        Meal meal = new Meal();
        meal.setUserId(userId);
        applyFields(meal, request.mealType(), request.description(), request.mealTime(),
                request.totalCalories(), request.proteinGrams(), request.carbohydrateGrams(),
                request.fatGrams(), request.fibreGrams(), request.notes());
        meal.setDeleted(false);
        return wellnessMapper.toMealResponse(mealRepository.save(meal));
    }

    @Transactional(readOnly = true)
    public MealResponse getById(UUID userId, UUID id) {
        return wellnessMapper.toMealResponse(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MealResponse> list(
            UUID userId, Instant from, Instant to, MealType mealType, Pageable pageable) {
        Page<Meal> page = mealRepository.findAll(
                MealRepository.withFilters(userId, from, to, mealType), pageable);
        List<MealResponse> content = page.getContent().stream()
                .map(wellnessMapper::toMealResponse)
                .toList();
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public MealResponse update(UUID userId, UUID id, UpdateMealRequest request) {
        validateNutrition(request.totalCalories(), request.proteinGrams(), request.carbohydrateGrams(),
                request.fatGrams(), request.fibreGrams());
        Meal meal = requireOwned(userId, id);
        applyFields(meal, request.mealType(), request.description(), request.mealTime(),
                request.totalCalories(), request.proteinGrams(), request.carbohydrateGrams(),
                request.fatGrams(), request.fibreGrams(), request.notes());
        return wellnessMapper.toMealResponse(mealRepository.save(meal));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Meal meal = requireOwned(userId, id);
        meal.softDelete();
        mealRepository.save(meal);
    }

    private Meal requireOwned(UUID userId, UUID id) {
        return mealRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(MealNotFoundException::new);
    }

    private void applyFields(
            Meal meal,
            MealType mealType,
            String description,
            Instant mealTime,
            Double totalCalories,
            Double proteinGrams,
            Double carbohydrateGrams,
            Double fatGrams,
            Double fibreGrams,
            String notes) {
        meal.setMealType(mealType);
        meal.setDescription(description.trim());
        meal.setMealTime(mealTime);
        meal.setTotalCalories(totalCalories);
        meal.setProteinGrams(proteinGrams);
        meal.setCarbohydrateGrams(carbohydrateGrams);
        meal.setFatGrams(fatGrams);
        meal.setFibreGrams(fibreGrams);
        meal.setNotes(trimToNull(notes));
    }

    static void validateNutrition(Double... values) {
        for (Double value : values) {
            if (value != null && value < 0) {
                throw new IllegalArgumentException("Nutrition values cannot be negative");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
