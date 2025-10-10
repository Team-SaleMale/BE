package com.salemale.domain.chat.dto; // 채팅 관련 요청/응답 데이터를 담는 DTO 클래스 모음

import lombok.*; // 게터 등 생성
import java.time.LocalDateTime; // 날짜와 시간을 다루기 위함

/*
 ChatDtos
 - 채팅방과 관련된 데이터 전송용 클래스 모음
 - Controller ↔ Service
 */
public class ChatDtos {

    /*
     채팅방 생성 요청 DTO
     - 클라이언트가 새로운 채팅방을 만들 때 필요한 정보 전달
     - itemId: 상품 ID
     - sellerId: 판매자 ID
     - buyerId: 구매자 ID (생략 가능, 현재 로그인한 사용자가 buyer일 경우 자동 처리)
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateChatRequest {
        private Long itemId;   // 상품 ID
        private Long sellerId; // 판매자 ID
        private Long buyerId;  // 구매자 ID
    }

    /*
     채팅방 목록 요약 정보 DTO
     - 채팅방 리스트 화면에서 보여질 정보
     - unreadCount: 읽지 않은 메시지 개수
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatSummary {
        private Long chatId;           // 채팅방 ID
        private Long itemId;           // 상품 ID
        private Long sellerId;         // 판매자 ID
        private Long buyerId;          // 구매자 ID
        private LocalDateTime lastMessageAt; // 마지막 메시지 시각
        private Long unreadCount;      // 안 읽은 메시지 개수
    }

    /*
     채팅방 생성 결과 DTO
     - 새로 만든 채팅방의 ID만 반환 (생성 후 바로 입장 가능)
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatResponse {
        private Long chatId; // 생성된 채팅방 ID
    }
}
