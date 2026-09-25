package com.digitallifetwin.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String answer,
        String engine,
        String provider,
        String model,
        String proposedAction,
        String disclaimer
) {
}
