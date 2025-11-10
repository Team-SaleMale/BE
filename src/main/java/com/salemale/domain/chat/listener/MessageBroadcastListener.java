package com.salemale.domain.chat.listener;

import com.salemale.domain.chat.service.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBroadcastListener {

    // SimpMessagingTemplate 직접 주입 금지
    // 필요 시점에만 지연 조회(순환 의존 완전 차단)
    private final ObjectProvider<SimpMessagingTemplate> templateProvider;

    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        var dto = event.getPayload();

        // 필요할 때만 실제 빈 조회 (없으면 null)
        SimpMessagingTemplate template = templateProvider.getIfAvailable();
        if (template == null) {
            log.warn("[WS] SimpMessagingTemplate not available yet. skip broadcast. chatId={}, messageId={}",
                    dto.getChatId(), dto.getMessageId());
            return;
        }

        try {
            template.convertAndSend("/topic/chats/" + dto.getChatId(), dto);
            log.debug("[WS] broadcast chatId={} messageId={}", dto.getChatId(), dto.getMessageId());
        } catch (Exception e) {
            log.warn("[WS] broadcast failed chatId={} cause={}", dto.getChatId(), e.getMessage());
        }
    }
}
