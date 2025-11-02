package com.salemale.domain.chat.dto; // 채팅 관련 요청/응답 데이터를 담는 DTO 클래스 모음

import com.salemale.domain.chat.entity.Message.MessageType;
import lombok.*; // 게터 등 생성
import java.time.LocalDateTime; // 날짜와 시간을 다루기 위함
import java.util.List;

/**
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
     (변경 전) 채팅방 목록 요약 정보 DTO
     - 채팅방 리스트 화면에서 보여질 정보
     - unreadCount: 읽지 않은 메시지 개수
     */
    /*
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatSummary {
        private Long chatId;           // 채팅방 ID
        private Long itemId;           // 상품 ID
        private Long sellerId;         // 판매자 ID
        private Long buyerId;          // 구매자 ID
        private LocalDateTime lastMessageAt; // 마지막 메시지 시각
        private Long unreadCount;      // 안 읽은 메시지 개수
    }

     */

    /**채팅방 목록 : chatId + unreadCount
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatIdUnread {
        private Long chatId;
        private Long unreadCount;
    }

    /**
     채팅방 생성 결과 DTO
     - 새로 만든 채팅방의 ID만 반환 (생성 후 바로 입장 가능)
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatResponse {
        private Long chatId; // 생성된 채팅방 ID
    }

    // 채팅방 입장 응답: 읽지 않은 메시지 일괄 처리 결과 + 메시지 목록(오름차순)
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatEnterResponse {
        private Long chatId;
        private Long readerId;
        private int updatedCount;        // 이번에 읽음 처리된 개수
        private int unreadCountAfter;    // 처리 후 남은 미읽음(보통 0)
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private List<MessageBrief> messages;
        private boolean canSend;   // 메시지 전송 가능 여부
    }

    // 입장 시 반환할 메시지 간략 DTO (오름차순)
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MessageBrief {
        private Long messageId;
        private Long senderId;
        private String content;
        private MessageType type;
        private boolean read;
        private LocalDateTime sentAt;
    }
}
