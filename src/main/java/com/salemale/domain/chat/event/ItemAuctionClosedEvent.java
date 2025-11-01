package com.salemale.domain.chat.event;

/*
 경매가 정상 종료되어 winner가 확정되었음을 알리는 도메인 이벤트.
 - Chat 도메인에서 수신하여 채팅방 자동 생성에 사용
 */
public record ItemAuctionClosedEvent(Long itemId) {}
