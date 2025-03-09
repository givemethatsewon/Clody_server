package com.clody.clodyapi.diary.controller.dto.response;

import java.time.LocalDate;

public record DiaryCreatedTimeResponse(
    int HH,
    int mm,
    int ss,
    LocalDate date,
    boolean isFirst,
    boolean isFromAd
) {
    public static DiaryCreatedTimeResponse of(int HH, int mm, int ss,LocalDate date, boolean isFirst, boolean isFromAd) {
        return new DiaryCreatedTimeResponse(HH, mm, ss, date, isFirst, isFromAd);
    }

    // 기존 메서드 오버로딩 - 호환성 유지
    public static DiaryCreatedTimeResponse of(int HH, int mm, int ss, LocalDate date, boolean isFirst) {
        return of(HH, mm, ss, date, isFirst, false);
    }
}
