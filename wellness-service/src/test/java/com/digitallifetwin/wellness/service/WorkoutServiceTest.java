package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WorkoutServiceTest {

    @Test
    void validateDuration_rejectsInvalid() {
        assertThatThrownBy(() -> WorkoutService.validateDuration(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duration");
        assertThatThrownBy(() -> WorkoutService.validateDuration(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
