package com.salemale.global.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@Slf4j
public class StompUserInterceptor implements ChannelInterceptor {

    private static class SimplePrincipal implements Principal {
        private final String name;
        SimplePrincipal(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public String toString() { return name; }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        // CONNECT: user_id principal로 심는다
        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            String userId = acc.getFirstNativeHeader("user_id");
            log.info("[WS] CONNECT headers={} user_id={}", acc.toNativeHeaderMap(), userId);
            if (userId != null && !userId.isBlank()) {
                acc.setUser(new SimplePrincipal(userId));
            }
        } else {
            // 그 외 프레임: 세션에 보관된 simpUser가 헤더에 실려오는데,
            // 혹시 비어있으면 다시 매핑해준다(환경 따라 null이 되는 경우 방지)
            if (acc.getUser() == null) {
                Object simpUser = message.getHeaders().get(SimpMessageHeaderAccessor.USER_HEADER);
                if (simpUser instanceof Principal p) {
                    acc.setUser(p);
                }
            }
        }
        return message;
    }
}
