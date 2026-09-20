package com.digitallifetwin.auth.dto.response;

import java.util.List;
import java.util.Map;

public record AssistantConversationsResponse(List<Map<String, Object>> conversations) {
}
