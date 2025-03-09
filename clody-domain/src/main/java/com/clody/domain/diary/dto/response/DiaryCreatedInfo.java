package com.clody.domain.diary.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryCreatedInfo(
    int HH,
    int MM,
    int SS,
    LocalDate date,
    boolean isFirst,
    boolean isFromAd
) {
    public static DiaryCreatedInfo from(LocalDateTime createdAt, boolean isFirst, boolean isFromAd) {
        return new DiaryCreatedInfo(createdAt.getHour(), createdAt.getMinute(), createdAt.getSecond(), createdAt.toLocalDate(), isFirst, isFromAd);
    }

    // 기존 코드와의 호환성을 위한 오버로딩 메서드
    public static DiaryCreatedInfo from(LocalDateTime createdAt, boolean isFirst) {
        return from(createdAt, isFirst, false);
    }

    public DiaryCreatedInfo withIsFromAdTrue() {
        return new DiaryCreatedInfo(HH, MM, SS, date, isFirst, true);
    }
}
