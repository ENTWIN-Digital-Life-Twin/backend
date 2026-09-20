package com.digitallifetwin.auth.dto.request;

import java.util.List;
import java.util.Map;

public record UpdateAssistantConversationsRequest(
        List<Map<String, Object>> conversations
) {
}
