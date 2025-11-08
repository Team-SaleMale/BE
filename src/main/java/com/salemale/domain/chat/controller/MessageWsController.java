package com.salemale.domain.chat.controller;

import com.salemale.domain.chat.dto.MessageDtos;
import com.salemale.domain.chat.dto.MessageDtos.MessageResponse;
import com.salemale.domain.chat.dto.MessageDtos.SendMessageRequest;
import com.salemale.domain.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageWsController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트 발행:   /app/chats/{chatId}/send
     * 서버 브로드캐스트: /topic/chats/{chatId}
     * payload 예시)
     * {
     *   "content": "안녕!",
     *   "type": "TEXT"
     * }
     * USER_ID는 연결 시 헤더로 보내고, StompUserInterceptor가 Principal로 한다.
     */
    @MessageMapping("/chats/{chatId}/send")
    public void sendToChat(
            @DestinationVariable Long chatId,
            @Payload SendMessageRequest payload,
            Principal principal
    ) {
        // 1) 인증 사용자 (StompUserInterceptor에서 USER_ID를 Principal.name으로 설정)
        Long me = Long.valueOf(principal.getName());

        // 2) 경로의 chatId를 우선 사용
        SendMessageRequest req = SendMessageRequest.builder()
                .chatId(chatId)
                .content(payload.getContent())
                .type(payload.getType())
                .build();

        // 3) 저장
        MessageResponse saved = messageService.send(me, req);

        // 4) 해당 채팅방 구독자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chats/" + chatId, saved);
    }
}