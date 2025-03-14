package com.clody.clodyapi.reply.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReplyAdRequest(
        @Schema(description = "대상 연도", example = "2025")
        int year,

        @Schema(description = "대상 달", example = "3")
        int month,

        @Schema(description = "대상 일", example = "14")
        int date
) {
    public static ReplyAdRequest of(int year, int month, int date) {
        return new ReplyAdRequest(year, month, date);
    }
}
