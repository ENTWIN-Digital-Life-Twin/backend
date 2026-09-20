package com.digitallifetwin.auth.entity;

import com.digitallifetwin.auth.enums.TransportMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "daily_water_goal_ml")
    private Integer dailyWaterGoalMl;

    @Column(name = "preferred_sleep_time")
    private LocalTime preferredSleepTime;

    @Column(name = "preferred_wake_time")
    private LocalTime preferredWakeTime;

    @Column(name = "preferred_workout_time")
    private LocalTime preferredWorkoutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_transport_mode", length = 50)
    private TransportMode defaultTransportMode;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "email_notification_enabled", nullable = false)
    private boolean emailNotificationEnabled;

    @Column(name = "push_notification_enabled", nullable = false)
    private boolean pushNotificationEnabled;

    @Column(name = "ai_recommendation_enabled", nullable = false)
    private boolean aiRecommendationEnabled;

    @Column(name = "traffic_integration_enabled", nullable = false)
    private boolean trafficIntegrationEnabled;

    @Column(name = "ui_settings", columnDefinition = "TEXT")
    private String uiSettings;

    @Column(name = "assistant_conversations", columnDefinition = "TEXT")
    private String assistantConversations;

    public static UserPreference defaultsFor(User user) {
        UserPreference preference = new UserPreference();
        preference.setUser(user);
        preference.setDailyWaterGoalMl(2000);
        preference.setPreferredSleepTime(LocalTime.of(23, 0));
        preference.setPreferredWakeTime(LocalTime.of(7, 0));
        preference.setPreferredWorkoutTime(LocalTime.of(18, 0));
        preference.setDefaultTransportMode(TransportMode.WALK);
        preference.setNotificationEnabled(true);
        preference.setEmailNotificationEnabled(true);
        preference.setPushNotificationEnabled(true);
        preference.setAiRecommendationEnabled(true);
        preference.setTrafficIntegrationEnabled(false);
        return preference;
    }
}
