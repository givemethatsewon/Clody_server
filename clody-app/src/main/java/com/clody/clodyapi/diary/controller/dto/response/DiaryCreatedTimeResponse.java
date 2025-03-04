package com.clody.clodyapi.diary.controller.dto.response;

import java.time.LocalDate;

public record DiaryCreatedTimeResponse(
    int HH,
    int mm,
    int ss,
    LocalDate date,
    boolean isFirst
) {
    public static DiaryCreatedTimeResponse of(int HH, int mm, int ss,LocalDate date, boolean isFirst) {
        return new DiaryCreatedTimeResponse(HH, mm, ss, date, isFirst);
    }
}
