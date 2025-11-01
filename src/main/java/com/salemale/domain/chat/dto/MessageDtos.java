package com.salemale.domain.chat.dto; // 메시지 관련 요청/응답 DTO 클래스 모음

import com.salemale.domain.chat.entity.Message.MessageType; // 메시지 종류(TEXT, IMAGE, URL)
import lombok.*; // 게터 등 생성
import java.time.LocalDateTime; // 메시지 보낸 시간 기록용

/*
 MessageDtos
 - 메시지 전송 및 조회 관련 DTO 모음
 - Controller ↔ Service 간 데이터 교환용
 */
public class MessageDtos {

    /*
     메시지 전송 요청 DTO
     - 사용자가 메시지를 보낼 때 필요한 데이터
     - chatId: 어느 채팅방에 보낼지
     - content: 보낼 내용
     - type: 메시지 종류(TEXT / IMAGE / URL)
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendMessageRequest {
        private Long chatId;        // 채팅방 ID
        private String content;     // 메시지 내용
        private MessageType type;   // 메시지 타입 (TEXT / IMAGE / URL)
    }

    /*
     메시지 전송 결과 또는 메시지 조회 DTO
     - 메시지 목록 또는 보낸 결과를 클라이언트에 전달
     */
    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MessageResponse {
        private Long messageId;         // 메시지 ID
        private Long chatId;            // 채팅방 ID
        private Long senderId;          // 보낸 사람 ID
        private String content;         // 메시지 내용
        private MessageType type;       // 메시지 타입(TEXT/IMAGE/URL)
        private boolean read;           // 읽음 여부
        private LocalDateTime sentAt;   // 메시지 전송 시각
    }
}
