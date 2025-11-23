package com.salemale.domain.item.service;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemTransaction;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.item.repository.ItemTransactionRepository;
import com.salemale.global.common.enums.ItemStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.salemale.domain.chat.event.ItemAuctionClosedEvent; //채팅방 생성을 위해 추가
import org.springframework.context.ApplicationEventPublisher; // 채팅방 생성을 위해 추가

import com.salemale.domain.alarm.service.AlarmService;       // 알람 생성을 위해 추가
import com.salemale.domain.alarm.dto.AlarmDtos.CreateAlarmRequest; // 알람 생성을 위해 추가


@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSchedulerService {

    private final ItemRepository itemRepository;
    private final ItemTransactionRepository itemTransactionRepository;

    private final ApplicationEventPublisher publisher; //채팅방 생성을 위해 추가
    private final AlarmService alarmService; //알람 생성을 위해 추가

    // 1분마다 종료된 경매 처리 로직
    @Scheduled(fixedRate = 60000) // 60000ms = 1분
    @Transactional
    public void processExpiredAuctions() {
        log.info("경매 종료 처리 시작");

        // 1. 종료 시간이 지난 BIDDING 상태 경매들 조회
        LocalDateTime now = LocalDateTime.now();
        List<Item> expiredItems = itemRepository.findByEndTimeBeforeAndItemStatus(
                now,
                ItemStatus.BIDDING
        );

        log.info("종료 대상 경매: {}건", expiredItems.size());

        // 2. 각 경매별로 처리
        for (Item item : expiredItems) {
            processAuction(item);
        }

        log.info("경매 종료 처리 완료");
    }

    private void processAuction(Item item) {
        // 입찰 여부 확인
        boolean hasBids = itemTransactionRepository.existsByItem(item);

        if (hasBids) {
            // 입찰이 있으면 최고 입찰자를 낙찰자로 설정
            ItemTransaction winningBid = itemTransactionRepository
                    .findTopByItemOrderByBidPriceDescCreatedAtAsc(item)
                    .orElseThrow();

            item.completeAuction(winningBid.getBuyer());
            log.info("경매 낙찰 처리: itemId={}, winnerId={}, finalPrice={}",
                    item.getItemId(),
                    winningBid.getBuyer().getId(),
                    winningBid.getBidPrice());

            // 알림 생성 (판매자 + 낙찰자)
            Long sellerId = item.getSeller().getId();
            Long winnerId = winningBid.getBuyer().getId();
            String title = item.getTitle();

            // 판매자에게
            String msgForSeller = "경매가 낙찰되었습니다.";
            alarmService.createAlarm(new CreateAlarmRequest(sellerId, msgForSeller));

            // 낙찰자에게
            String msgForWinner = "축하합니다! 경매에 낙찰되었습니다.";
            alarmService.createAlarm(new CreateAlarmRequest(winnerId, msgForWinner));


            // 트랜잭션 커밋 후 채팅 자동 생성, 채팅방 생성을 위해 추가
            publisher.publishEvent(new ItemAuctionClosedEvent(item.getItemId()));


        } else {
            // 입찰이 없으면 유찰 처리
            item.failAuction();
            log.info("경매 유찰 처리: itemId={}", item.getItemId());

            // 알림 생성 (판매자만)
            Long sellerId = item.getSeller().getId();
            String title = item.getTitle();

            String msgForSeller = "경매가 유찰되었습니다.";
            alarmService.createAlarm(new CreateAlarmRequest(sellerId, msgForSeller));
        }
    }
}