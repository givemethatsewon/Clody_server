package com.clody.domain.alarm.event;

import com.clody.domain.alarm.Alarm;
import com.clody.domain.alarm.dto.ScheduleAlarmInfo;
import com.clody.domain.alarm.repository.AlarmRepository;
import com.clody.domain.alarm.service.AlarmScheduler;
import com.clody.domain.alarm.strategy.ScheduleAlarmTimeFactory;
import com.clody.domain.alarm.strategy.ScheduleTimeStrategy;
import com.clody.domain.reply.ReplyType;
import com.clody.domain.reply.dto.CreationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyCompletionHandler {

  private final AlarmRepository alarmRepository;
  private final ScheduleAlarmTimeFactory alarmTimeFactory;
  private final AlarmScheduler alarmScheduler;

  @EventListener
  public void handleReplyCompletion(CreationMessage event) {
    /*
    * 사용자가 알람 수신에 동의한 경우, 알림 스케줄에 등록
     */
    Alarm alarm = alarmRepository.findByUserId(event.userId());

    // 추가: 광고로 인한 즉시 답변의 경우 알림 스케줄 생성 skip (임시로 -1로 처리
    if (event.type() == ReplyType.DYNAMIC && event.version() == -1) {
        log.info("광고 답장의 경우 알림 스케줄 스킵: {}", event);
        return;
    }


    //TODO 트랜잭션 분리해야 합니다. 
    if(!alarm.checkUserAgreedForReplyAlarm()) return;

    ScheduleTimeStrategy scheduleTimeStrategy = alarmTimeFactory.getStrategy(event.type());
    scheduleTimeStrategy.calculateAlarmDelayTime()
        .map(rt -> ScheduleAlarmInfo.of(event, rt, alarm.getFcmToken()))
        .ifPresent(info -> {
          alarmScheduler.scheduleReplyPushAlarm(info);
          log.info("알림 스케줄 정상 등록: {}", info);
        });
  }
}
