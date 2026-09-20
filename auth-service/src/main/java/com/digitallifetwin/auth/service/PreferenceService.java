package com.digitallifetwin.auth.service;

import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.AccessibilitySettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.AppearanceSettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.FormatSettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.NotificationSettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.PrivacySettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.ProfilePrefs;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.WellnessTargets;
import com.digitallifetwin.auth.dto.response.AssistantConversationsResponse;
import com.digitallifetwin.auth.dto.response.PreferenceResponse;
import com.digitallifetwin.auth.entity.User;
import com.digitallifetwin.auth.entity.UserPreference;
import com.digitallifetwin.auth.exception.UserNotFoundException;
import com.digitallifetwin.auth.repository.UserPreferenceRepository;
import com.digitallifetwin.auth.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final TypeReference<UpdatePreferencesRequest> SETTINGS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> CONVERSATIONS_TYPE = new TypeReference<>() {
    };
    private static final int MAX_CONVERSATIONS = 50;

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PreferenceResponse getPreferences(UUID userId) {
        User user = requireUser(userId);
        UserPreference preference = requirePreference(user);
        UpdatePreferencesRequest stored = readSettings(preference.getUiSettings());
        UpdatePreferencesRequest merged = merge(defaults(), stored);
        return toResponse(user.getBio(), preference.getDailyWaterGoalMl(), merged);
    }

    @Transactional
    public PreferenceResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        User user = requireUser(userId);
        UserPreference preference = requirePreference(user);
        UpdatePreferencesRequest merged = merge(defaults(), merge(readSettings(preference.getUiSettings()), request));
        preference.setUiSettings(writeJson(merged));
        if (merged.wellnessTargets() != null && merged.wellnessTargets().waterTarget() != null) {
            preference.setDailyWaterGoalMl((int) Math.round(merged.wellnessTargets().waterTarget() * 1000));
        }
        applyNotificationFlags(preference, merged.notifications());
        userPreferenceRepository.save(preference);
        return toResponse(user.getBio(), preference.getDailyWaterGoalMl(), merged);
    }

    @Transactional(readOnly = true)
    public AssistantConversationsResponse getConversations(UUID userId) {
        UserPreference preference = requirePreference(requireUser(userId));
        return new AssistantConversationsResponse(readConversations(preference.getAssistantConversations()));
    }

    @Transactional
    public AssistantConversationsResponse saveConversations(UUID userId, List<Map<String, Object>> conversations) {
        UserPreference preference = requirePreference(requireUser(userId));
        List<Map<String, Object>> safe = conversations == null ? List.of() : conversations;
        if (safe.size() > MAX_CONVERSATIONS) {
            safe = new ArrayList<>(safe.subList(0, MAX_CONVERSATIONS));
        }
        preference.setAssistantConversations(writeJson(safe));
        userPreferenceRepository.save(preference);
        return new AssistantConversationsResponse(safe);
    }

    private User requireUser(UUID userId) {
        return userRepository.findByIdWithRoles(userId).orElseThrow(UserNotFoundException::new);
    }

    private UserPreference requirePreference(User user) {
        return userPreferenceRepository.findByUser_Id(user.getId()).orElseGet(() -> {
            UserPreference created = UserPreference.defaultsFor(user);
            user.assignPreference(created);
            return userPreferenceRepository.save(created);
        });
    }

    private UpdatePreferencesRequest readSettings(String json) {
        if (json == null || json.isBlank()) {
            return defaults();
        }
        try {
            return merge(defaults(), objectMapper.readValue(json, SETTINGS_TYPE));
        } catch (JsonProcessingException ex) {
            return defaults();
        }
    }

    private List<Map<String, Object>> readConversations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(json, CONVERSATIONS_TYPE);
            return parsed == null ? List.of() : parsed;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize preferences", ex);
        }
    }

    private void applyNotificationFlags(UserPreference preference, NotificationSettings notifications) {
        if (notifications == null) {
            return;
        }
        if (notifications.taskReminders() != null || notifications.eventReminders() != null
                || notifications.wellnessReminders() != null) {
            boolean enabled = Boolean.TRUE.equals(notifications.taskReminders())
                    || Boolean.TRUE.equals(notifications.eventReminders())
                    || Boolean.TRUE.equals(notifications.wellnessReminders());
            preference.setNotificationEnabled(enabled);
            preference.setPushNotificationEnabled(enabled);
        }
        if (notifications.dailySummary() != null) {
            preference.setEmailNotificationEnabled(notifications.dailySummary());
        }
        if (notifications.aiInsights() != null) {
            preference.setAiRecommendationEnabled(notifications.aiInsights());
        }
    }

    static UpdatePreferencesRequest defaults() {
        return new UpdatePreferencesRequest(
                new AppearanceSettings("system", "teal"),
                new NotificationSettings(true, true, true, true, true, "morning"),
                new FormatSettings("long", "monday"),
                new PrivacySettings(true, true, true),
                new AccessibilitySettings(false, false, "normal", true),
                new ProfilePrefs(true, true, false, "22:00", "07:00"),
                new WellnessTargets(8.0, 2.5, 45)
        );
    }

    static UpdatePreferencesRequest merge(UpdatePreferencesRequest base, UpdatePreferencesRequest patch) {
        if (patch == null) {
            return base;
        }
        AppearanceSettings appearance = patch.appearance() != null
                ? new AppearanceSettings(
                first(patch.appearance().theme(), base.appearance().theme()),
                first(patch.appearance().accent(), base.appearance().accent()))
                : base.appearance();
        NotificationSettings notifications = patch.notifications() != null
                ? new NotificationSettings(
                first(patch.notifications().taskReminders(), base.notifications().taskReminders()),
                first(patch.notifications().eventReminders(), base.notifications().eventReminders()),
                first(patch.notifications().wellnessReminders(), base.notifications().wellnessReminders()),
                first(patch.notifications().aiInsights(), base.notifications().aiInsights()),
                first(patch.notifications().dailySummary(), base.notifications().dailySummary()),
                first(patch.notifications().summaryFrequency(), base.notifications().summaryFrequency()))
                : base.notifications();
        FormatSettings format = patch.preferences() != null
                ? new FormatSettings(
                first(patch.preferences().dateFormat(), base.preferences().dateFormat()),
                first(patch.preferences().weekStart(), base.preferences().weekStart()))
                : base.preferences();
        PrivacySettings privacy = patch.privacy() != null
                ? new PrivacySettings(
                first(patch.privacy().analytics(), base.privacy().analytics()),
                first(patch.privacy().personalization(), base.privacy().personalization()),
                first(patch.privacy().aiContext(), base.privacy().aiContext()))
                : base.privacy();
        AccessibilitySettings accessibility = patch.accessibility() != null
                ? new AccessibilitySettings(
                first(patch.accessibility().reduceMotion(), base.accessibility().reduceMotion()),
                first(patch.accessibility().highContrast(), base.accessibility().highContrast()),
                first(patch.accessibility().textSize(), base.accessibility().textSize()),
                first(patch.accessibility().focusKeyboard(), base.accessibility().focusKeyboard()))
                : base.accessibility();
        ProfilePrefs profilePrefs = patch.profilePrefs() != null
                ? new ProfilePrefs(
                first(patch.profilePrefs().activitySummary(), base.profilePrefs().activitySummary()),
                first(patch.profilePrefs().wellnessReminders(), base.profilePrefs().wellnessReminders()),
                first(patch.profilePrefs().quietHoursEnabled(), base.profilePrefs().quietHoursEnabled()),
                first(patch.profilePrefs().quietStart(), base.profilePrefs().quietStart()),
                first(patch.profilePrefs().quietEnd(), base.profilePrefs().quietEnd()))
                : base.profilePrefs();
        WellnessTargets wellness = patch.wellnessTargets() != null
                ? new WellnessTargets(
                first(patch.wellnessTargets().sleepTarget(), base.wellnessTargets().sleepTarget()),
                first(patch.wellnessTargets().waterTarget(), base.wellnessTargets().waterTarget()),
                first(patch.wellnessTargets().activeMinutesTarget(), base.wellnessTargets().activeMinutesTarget()))
                : base.wellnessTargets();
        return new UpdatePreferencesRequest(
                appearance, notifications, format, privacy, accessibility, profilePrefs, wellness);
    }

    private static <T> T first(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static PreferenceResponse toResponse(String bio, Integer waterGoal, UpdatePreferencesRequest settings) {
        return new PreferenceResponse(
                bio,
                waterGoal,
                settings.appearance(),
                settings.notifications(),
                settings.preferences(),
                settings.privacy(),
                settings.accessibility(),
                settings.profilePrefs(),
                settings.wellnessTargets()
        );
    }
}
