package com.salemale.global.ws;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class StompUserInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String userId = accessor.getFirstNativeHeader("USER_ID"); // 클라이언트 헤더
        if (userId != null && accessor.getUser() == null) {
            accessor.setUser(new Principal() {
                @Override public String getName() { return userId; }
            });
        }
        return message;
    }
}
