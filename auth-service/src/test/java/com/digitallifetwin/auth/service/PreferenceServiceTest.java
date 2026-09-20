package com.digitallifetwin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.AppearanceSettings;
import com.digitallifetwin.auth.dto.response.PreferenceResponse;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.entity.UserPreference;
import com.digitallifetwin.auth.repository.UserPreferenceRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    private PreferenceService preferenceService;
    private UUID userId;
    private User user;
    private UserPreference preference;

    @BeforeEach
    void setUp() {
        preferenceService = new PreferenceService(userRepository, userPreferenceRepository, new ObjectMapper());
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setBio("Hello");
        preference = UserPreference.defaultsFor(user);
        preference.setId(UUID.randomUUID());
    }

    @Test
    void getPreferences_returnsDefaultsWhenEmpty() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUser_Id(userId)).thenReturn(Optional.of(preference));

        PreferenceResponse response = preferenceService.getPreferences(userId);

        assertThat(response.bio()).isEqualTo("Hello");
        assertThat(response.appearance().theme()).isEqualTo("system");
        assertThat(response.wellnessTargets().sleepTarget()).isEqualTo(8.0);
        assertThat(response.notifications().aiInsights()).isTrue();
    }

    @Test
    void updatePreferences_persistsThemeAndWaterGoal() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUser_Id(userId)).thenReturn(Optional.of(preference));
        when(userPreferenceRepository.save(preference)).thenReturn(preference);

        PreferenceResponse response = preferenceService.updatePreferences(userId, new UpdatePreferencesRequest(
                new AppearanceSettings("dark", "navy"),
                null, null, null, null, null,
                new UpdatePreferencesRequest.WellnessTargets(7.5, 3.0, 60)
        ));

        assertThat(response.appearance().theme()).isEqualTo("dark");
        assertThat(response.appearance().accent()).isEqualTo("navy");
        assertThat(preference.getDailyWaterGoalMl()).isEqualTo(3000);
        verify(userPreferenceRepository).save(preference);
        assertThat(preference.getUiSettings()).contains("dark");
    }

    @Test
    void saveConversations_capsList() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUser_Id(userId)).thenReturn(Optional.of(preference));
        when(userPreferenceRepository.save(preference)).thenReturn(preference);

        List<Map<String, Object>> payload = List.of(Map.of("id", "c-1", "titleKey", "assistantPage.newDiscussion"));
        var saved = preferenceService.saveConversations(userId, payload);

        assertThat(saved.conversations()).hasSize(1);
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getAssistantConversations()).contains("c-1");
    }

    @Test
    void getConversations_emptyWhenUnset() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUser_Id(userId)).thenReturn(Optional.of(preference));

        assertThat(preferenceService.getConversations(userId).conversations()).isEmpty();
    }

    @Test
    void getPreferences_createsDefaultRowWhenMissing() {
        when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PreferenceResponse response = preferenceService.getPreferences(userId);

        assertThat(response.preferences().weekStart()).isEqualTo("monday");
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }
}
