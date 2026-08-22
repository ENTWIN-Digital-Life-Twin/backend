package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MealServiceTest {

    @Test
    void validateNutrition_rejectsNegative() {
        assertThatThrownBy(() -> MealService.validateNutrition(100.0, -1.0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }
}
