package com.salemale.domain.chat.listener;

import com.salemale.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSubscribeListener {

    private final ChatService chatService;

    /**
     * STOMP SUBSCRIBE 이벤트 리스너
     * - /topic/chats/{chatId} 로 구독 들어오면 해당 방의 '내가 안 읽은' 메시지를 일괄 읽음 처리
     * - 잘못된 목적지 형식/숫자 파싱 실패/권한 문제 등은 로그만 남기고 무시
     */
    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();   // 예: /topic/chats/123
        Principal principal = accessor.getUser();         // principal.getName() = USER_ID (StompUserInterceptor에서 설정)

        if (destination == null || principal == null) {
            return; // 정보 부족 시 무시
        }

        // /topic/chats/{chatId} 만 처리
        final String prefix = "/topic/chats/";
        if (!destination.startsWith(prefix)) {
            return;
        }

        try {
            // 쿼리스트링/슬래시 꼬리 등 방어적 파싱
            String path = destination.split("\\?")[0];         // ? 이후 제거
            String last = path.substring(prefix.length());     // "{chatId}" 또는 "{chatId}/..."
            if (last.contains("/")) {
                last = last.substring(0, last.indexOf('/'));   // 뒤에 붙은 어떤 세그먼트도 제거
            }

            Long chatId = Long.valueOf(last.trim());
            Long me = Long.valueOf(principal.getName().trim());

            // 읽음 처리 (권한/참여자 검증은 Service 내부에서 수행)
            chatService.markAllReadInChat(me, chatId);

            log.debug("[STOMP SUBSCRIBE] user={} subscribed to {}, marked unread as read.", me, destination);
        } catch (NumberFormatException e) {
            log.warn("[STOMP SUBSCRIBE] invalid chatId in destination: {}", destination);
        } catch (Exception e) {
            // 참여자가 아님, 이미 나간 방 등은 Service에서 IllegalState/EntityNotFound 등 던질 수 있음 → 로그만 남기고 무시
            log.info("[STOMP SUBSCRIBE] markAllRead skipped. dest={}, cause={}", destination, e.getMessage());
        }
    }
}
