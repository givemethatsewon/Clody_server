package com.clody.clodyapi.diary.usecase;

import com.clody.clodyapi.diary.controller.dto.response.DiaryCreatedTimeResponse;
import com.clody.clodyapi.diary.controller.dto.response.DiaryResponse;

public interface DiaryQueryUsecase {

  DiaryCreatedTimeResponse getCreatedTime(final int year, final int month, final int date);

  DiaryCreatedTimeResponse getCreatedTime(final int year, final int month, final int date, final Boolean adWatched); // 광고 시청 플래그 (adWatched) 추가

  DiaryResponse getDiary(final int year, final int month, final int date);
}
