package com.clody.clodyapi.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record SimpleSignUpRequest(
        @Schema(description = "이메일", example = "test@example.com")
        String email,

        @Schema(description = "사용자 이름", example = "테스트유저")
        String name,

        @Schema(description = "FCM 토큰 (선택)", example = "fcm_token_example")
        String fcmToken
) {
}
