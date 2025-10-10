package com.salemale.domain.chat.controller; // 메시지 관련 컨트롤러

import com.salemale.domain.chat.dto.MessageDtos.*; // 메시지 요청/응답 DTO 묶음
import com.salemale.domain.chat.service.MessageService; // 메시지 비즈니스 로직 서비스
import lombok.RequiredArgsConstructor; // 생성자 자동 생성
import org.springframework.http.ResponseEntity; // HTTP 응답 객체
import org.springframework.web.bind.annotation.*; // REST 매핑용 어노테이션

/*
 메시지 관련 API 컨트롤러
 - 메시지 전송
 - 메시지 읽음 처리
 */
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService; // 메시지 서비스 주입

    /*
     메시지 전송 API
     - 클라이언트에서 채팅방 ID, 내용, 타입(TEXT/IMAGE)을 받아 메시지를 생성한다.
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestHeader("EX_USER_ID") Long me,
            @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(messageService.send(me, request));
    }

    /*
     메시지 읽음 처리 API
     - 특정 메시지 ID 기준으로, 읽음 상태로 업데이트한다.
     */
    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<Void> readMessage(
            @RequestHeader("EX_USER_ID") Long me,
            @PathVariable Long id
    ) {
        messageService.markRead(me, id);
        return ResponseEntity.ok().build();
    }
}
