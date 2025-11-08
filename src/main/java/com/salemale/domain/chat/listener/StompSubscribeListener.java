package com.salemale.domain.chat.listener;

import com.salemale.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class StompSubscribeListener {

    private final ChatService chatService;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String dest = accessor.getDestination();      // /topic/chats/{chatId}
        Principal principal = accessor.getUser();     // USER_ID

        if (dest != null && dest.startsWith("/topic/chats/") && principal != null) {
            Long chatId = Long.valueOf(dest.substring("/topic/chats/".length()));
            Long me = Long.valueOf(principal.getName());
            // 입장 REST를 별도로 쓰면 이 부분은 선택
            chatService.markAllReadInChat(me, chatId);
        }
    }
}
