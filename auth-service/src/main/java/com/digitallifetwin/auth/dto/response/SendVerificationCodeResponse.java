package com.digitallifetwin.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendVerificationCodeResponse(String message, String verificationCode) {
}
