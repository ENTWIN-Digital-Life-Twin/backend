package com.digitallifetwin.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdatePreferencesRequest(
        AppearanceSettings appearance,
        NotificationSettings notifications,
        FormatSettings preferences,
        PrivacySettings privacy,
        AccessibilitySettings accessibility,
        ProfilePrefs profilePrefs,
        WellnessTargets wellnessTargets
) {

    public record AppearanceSettings(String theme, String accent) {
    }

    public record NotificationSettings(
            Boolean taskReminders,
            Boolean eventReminders,
            Boolean wellnessReminders,
            Boolean aiInsights,
            Boolean dailySummary,
            String summaryFrequency
    ) {
    }

    public record FormatSettings(String dateFormat, String weekStart) {
    }

    public record PrivacySettings(Boolean analytics, Boolean personalization, Boolean aiContext) {
    }

    public record AccessibilitySettings(
            Boolean reduceMotion,
            Boolean highContrast,
            String textSize,
            Boolean focusKeyboard
    ) {
    }

    public record ProfilePrefs(
            Boolean activitySummary,
            Boolean wellnessReminders,
            Boolean quietHoursEnabled,
            String quietStart,
            String quietEnd
    ) {
    }

    public record WellnessTargets(Double sleepTarget, Double waterTarget, Integer activeMinutesTarget) {
    }
}
