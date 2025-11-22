package com.salemale.domain.alarm.event;

import com.salemale.domain.alarm.dto.AlarmDtos.CreateAlarmRequest;
import com.salemale.domain.alarm.service.AlarmService;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.global.common.enums.ItemStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmEventListener {

    private final AlarmService alarmService;
    private final ItemRepository itemRepository;

    /**
     * 경매 종료 이벤트 -> 알림 생성
     * 스케줄러에서 publisher.publishEvent(new ItemAuctionClosedEvent(itemId)); 만 던지고 있으니까
     * 여기서는 itemId로 DB 조회해서 상태를 보고 메시지를 만든다.
     */
    @EventListener
    public void onItemAuctionClosed(com.salemale.domain.chat.event.ItemAuctionClosedEvent event) {
        Long itemId = extractItemId(event)
                .orElseThrow(() -> new IllegalArgumentException("ItemAuctionClosedEvent에 itemId가 없습니다."));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다. id=" + itemId));

        ItemStatus status = item.getItemStatus();   // COMPLETE, FAILED 등 (엔티티에 맞게)
        String title = item.getTitle();             // 아이템 제목 (필드명 다르면 여기만 수정)

        // 판매자 아이디 (필드명 다르면 여기만 수정)
        Long sellerId = item.getSeller().getId();

        String msg;
        if (status == ItemStatus.SUCCESS) {
            msg = "경매가 낙찰되었습니다: " + title;
        } else if (status == ItemStatus.FAIL) {
            msg = "경매가 유찰되었습니다: " + title;
        } else {
            // BIDDING 상태면 아직 경매 중인데 이벤트가 온 거라서 그냥 로그만 남기고 리턴해도 됨
            log.warn("BIDDING 상태에서 AuctionClosed 이벤트 수신: itemId={}", item.getItemId());
            return;
        }

        alarmService.createAlarm(new CreateAlarmRequest(sellerId, msg));

        log.info("AuctionClosed 알림 생성: itemId={}, status={}, sellerId={}",
                itemId, status, sellerId);
    }

    /**
     * record(itemId())든, 일반 클래스(getItemId())든 둘 다 지원하도록 리플렉션으로 꺼냄
     */
    private Optional<Long> extractItemId(Object evt) {
        // record 스타일: itemId()
        try {
            Method m = evt.getClass().getMethod("itemId");
            Object v = m.invoke(evt);
            return Optional.ofNullable((Long) v);
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            log.warn("itemId() 호출 실패: {}", evt.getClass(), e);
        }

        // POJO 스타일: getItemId()
        try {
            Method m = evt.getClass().getMethod("getItemId");
            Object v = m.invoke(evt);
            return Optional.ofNullable((Long) v);
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            log.warn("getItemId() 호출 실패: {}", evt.getClass(), e);
        }

        return Optional.empty();
    }

    // ---- 아래는 나중에 이벤트 만들면 추가할 템플릿 ----
    // @EventListener
    // public void onChatCreated(ChatCreatedEvent event) { ... }
    //
    // @EventListener
    // public void onNewMessage(NewMessageEvent event) { ... }
}
