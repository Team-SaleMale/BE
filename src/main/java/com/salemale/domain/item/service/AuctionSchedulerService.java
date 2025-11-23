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
import java.util.HashSet; //알람용 추가
import java.util.Set; // 알람용 추가

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

        // [알람용 추가] 종료 30~31분 전 경매에 알림 보내기
        processSoonToExpireAuctions(now);

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
            String msgForSeller = "경매가 낙찰되었습니다: " + title;
            alarmService.createAlarm(new CreateAlarmRequest(sellerId, msgForSeller));

            // 낙찰자에게
            String msgForWinner = "축하합니다! 경매에 낙찰되었습니다: " + title;
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

            String msgForSeller = "경매가 유찰되었습니다: " + title;
            alarmService.createAlarm(new CreateAlarmRequest(sellerId, msgForSeller));
        }
    }

    // [알람용 추가] 종료 30분 전 알림
    private void processSoonToExpireAuctions(LocalDateTime now) {
        LocalDateTime from = now.plusMinutes(30);
        LocalDateTime to = now.plusMinutes(31); // 1분 구간

        List<Item> soonEndingItems = itemRepository.findByEndTimeBetweenAndItemStatus(
                from,
                to,
                ItemStatus.BIDDING
        );

        if (soonEndingItems.isEmpty()) return;

        log.info("30분 전 알림 대상 경매: {}건", soonEndingItems.size());

        for (Item item : soonEndingItems) {
            sendPreEndAlarm(item);
        }
    }

    // [알람용 추가] 개별 아이템에 대한 30분 전 알림 생성
    private void sendPreEndAlarm(Item item) {
        Long sellerId = item.getSeller().getId();
        String title  = item.getTitle();

        // 1) 판매자에게 알림
        String msgForSeller = "경매 종료 30분 전입니다: " + title;
        alarmService.createAlarm(new CreateAlarmRequest(sellerId, msgForSeller));

        // 2) 모든 입찰 내역 조회
        List<ItemTransaction> bids = itemTransactionRepository.findByItem(item);
        if (bids.isEmpty()) {
            log.info("입찰자가 없어 30분 전 입찰자 알림 스킵: itemId={}", item.getItemId());
            return;
        }

        // 3) 현재 최고 입찰자 계산
        ItemTransaction highestBid = bids.stream()
                .sorted((a, b) -> {
                    int priceCompare = b.getBidPrice().compareTo(a.getBidPrice()); // 높은 금액 우선
                    if (priceCompare != 0) return priceCompare;
                    // 금액 같으면 더 먼저 입찰한 사람 우선
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .findFirst()
                .orElseThrow();

        Long highestBidderId = highestBid.getBuyer().getId();

        // 4) 같은 사람이 여러 번 입찰했을 수 있으니, 유저 기준으로 중복 제거
        Set<Long> notified = new HashSet<>();

        for (ItemTransaction bid : bids) {
            Long bidderId = bid.getBuyer().getId();

            // 판매자에게는 위에서 이미 보냈으니 제외
            if (bidderId.equals(sellerId)) continue;

            // 같은 유저가 여러 번 입찰했으면 한 번만 보냄
            if (!notified.add(bidderId)) continue;

            String msg;
            if (bidderId.equals(highestBidderId)) {
                // 현재 최고 입찰자 전용 메시지
                msg = "현재 최고 입찰가입니다. 경매 종료 30분 전입니다: " + title;
            } else {
                // 그냥 입찰에 참여한 사람 메시지
                msg = "참여 중인 경매 종료 30분 전입니다: " + title;
            }

            alarmService.createAlarm(new CreateAlarmRequest(bidderId, msg));
        }

        log.info("30분 전 알림 생성(판매자+입찰자): itemId={}, sellerId={}, bidders={}",
                item.getItemId(), sellerId, notified.size());
    }

}