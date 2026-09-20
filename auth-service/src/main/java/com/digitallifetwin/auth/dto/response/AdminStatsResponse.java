package com.digitallifetwin.auth.dto.response;

public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long adminUsers,
        long contactMessages
) {
}
