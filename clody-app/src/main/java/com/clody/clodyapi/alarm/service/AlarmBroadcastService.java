package com.clody.clodyapi.alarm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.clody.clodyapi.alarm.usecase.BroadCastAlarmUsecase;
import com.clody.domain.alarm.Alarm;
import com.clody.domain.alarm.repository.AlarmRepository;
import com.clody.domain.alarm.service.NotificationSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlarmBroadcastService implements BroadCastAlarmUsecase {

    private final AlarmRepository alarmRepository;
    private final NotificationSender alarmPublisher;

    @Override
    public boolean broadCastAlarm(String title, String body) {
        List<String> tokenList = alarmRepository.findAllAlarm().stream()
                                                .filter(alarm -> alarm.isDiaryAlarm())
                                                .map(Alarm::getFcmToken)
                                                .toList();
        alarmPublisher.broadcastAlarm(tokenList, title, body);
        return true;
    }
}
