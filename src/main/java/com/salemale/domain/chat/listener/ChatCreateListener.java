package com.salemale.domain.chat.listener;

import com.salemale.domain.chat.event.ItemAuctionClosedEvent;
import com.salemale.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/*
 낙찰(경매 종료) 이벤트를 수신하여
 ChatService를 통해 채팅방을 자동 생성/재사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCreateListener {

    private final ChatService chatService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionClosed(ItemAuctionClosedEvent event) {
        Long itemId = event.itemId();
        try {
            var resp = chatService.createChatForItemWinner(itemId);
            log.info("[CHAT] auto-created/reused chatId={} for itemId={}", resp.getChatId(), itemId);

            // 최초 안내 메시지 자동 전송
            // messageService.sendSystemMessage(resp.getChatId(), "낙찰 완료! 대화가 시작되었습니다.");
        } catch (Exception e) {
            // 이 트랜잭션은 커밋된 상태이므로, 실패해도 경매 확정에는 영향 없음
            log.error("[CHAT] auto-create failed for itemId={}", itemId, e);
        }
    }
}
