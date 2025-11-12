package com.salemale.domain.chat.controller;

import com.salemale.domain.chat.dto.MessageDtos;
import com.salemale.domain.chat.dto.MessageDtos.MessageResponse;
import com.salemale.domain.chat.dto.WsMessage;
import com.salemale.domain.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageWsController {

    private final MessageService messageService;

    @MessageMapping("/chats/{chatId}/send")
    @SendTo("/topic/chats/{chatId}")
    public MessageResponse sendToChat(@DestinationVariable Long chatId,
                                      @Payload WsMessage payload,
                                      Principal principal) {
        Long me = (principal != null) ? Long.valueOf(principal.getName()) : null;
        log.info("[WS] recv chatId={}, me={}, payload={}", chatId, me, payload);

        if (me == null) {
            // 여기까지 오면 인터셉터가 principal을 못 심은 것 → 인터셉터/연결 헤더 확인 필요
            log.warn("[WS] USER_ID missing (principal null) — drop message");
            return null; // @SendTo이므로 null이면 브로드캐스트되지 않음
        }

        MessageDtos.SendMessageRequest req = MessageDtos.SendMessageRequest.builder()
                .chatId(chatId)
                .content(payload.getContent())
                .type(payload.getType())
                .build();

        return messageService.send(me, req);
    }
}
