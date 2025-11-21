package com.salemale.domain.alarm.event;

import com.salemale.domain.alarm.service.AlarmService;
import com.salemale.domain.alarm.dto.AlarmDtos.CreateAlarmRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmEventListener {

    private final AlarmService alarmService;

    @EventListener
    public void onChatCreated(ChatCreatedEvent event) {
        String msg = "새 채팅방이 생성되었습니다 (chatId: " + event.chatId() + ")";
        alarmService.createAlarm(new CreateAlarmRequest(event.receiverId(), msg));
    }

    @EventListener
    public void onNewMessage(NewMessageEvent event) {
        String msg = "새 메시지가 도착했습니다: " + event.preview();
        alarmService.createAlarm(new CreateAlarmRequest(event.receiverId(), msg));
    }

    @EventListener
    public void onAuctionClosed(AuctionClosedEvent event) {
        String msg = event.isSold()
                ? "경매가 낙찰되었습니다! (" + event.itemTitle() + ")"
                : "경매가 유찰되었습니다. (" + event.itemTitle() + ")";
        alarmService.createAlarm(new CreateAlarmRequest(event.receiverId(), msg));
    }
}
