package com.clody.infra.external.fcm;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageContent {

  DIARY_WRITE_REQUEST("클로디","하루를 돌아보며 감사했던 순간을 적어 보세요 \uD83D\uDE4F"),
  REPLY_COMPLETED_REQUEST("클로디","로디가 전하는 행운의 답장이 도착했어요 \uD83D\uDC8C"),
  ;
  public final String title;
  public final String body;
}
