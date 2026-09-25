package com.digitallifetwin.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.scheduler")
public record NotificationSchedulerProperties(long fixedDelayMs) {
}
