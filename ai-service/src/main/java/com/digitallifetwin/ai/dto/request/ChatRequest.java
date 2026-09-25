package com.digitallifetwin.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatRequest(
        @JsonAlias("message")
        @NotBlank
        @Size(max = 4000)
        String question,
        Map<String, Object> context
) {
}
