package com.digitallifetwin.wellness.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoodServiceTest {

    @Test
    void validateLevels_rejectsOutOfRange() {
        assertThatThrownBy(() -> MoodService.validateLevels(0, 5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moodLevel");
        assertThatThrownBy(() -> MoodService.validateLevels(5, 11, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stressLevel");
    }
}
