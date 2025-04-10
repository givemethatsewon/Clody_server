package com.clody.domain.alarm.service;

import java.util.List;

public interface NotificationSender {

    void sendDiaryAlarm(String token);

    void sendReplyAlarm(String fcmToken);

    void broadcastAlarm(List<String> token, String title, String body);
}
