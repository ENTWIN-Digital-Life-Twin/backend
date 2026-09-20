package com.digitallifetwin.auth.dto.response;

import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.AccessibilitySettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.AppearanceSettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.FormatSettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.NotificationSettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.PrivacySettings;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.ProfilePrefs;
import com.digitallifetwin.auth.dto.request.UpdatePreferencesRequest.WellnessTargets;

public record PreferenceResponse(
        String bio,
        Integer dailyWaterGoalMl,
        AppearanceSettings appearance,
        NotificationSettings notifications,
        FormatSettings preferences,
        PrivacySettings privacy,
        AccessibilitySettings accessibility,
        ProfilePrefs profilePrefs,
        WellnessTargets wellnessTargets
) {
}
