package com.salemale.domain.chat.controller; // 메시지 관련 컨트롤러

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.chat.dto.MessageDtos.MessageResponse;
import com.salemale.domain.chat.dto.MessageDtos.*; // 메시지 요청/응답 DTO 묶음
import com.salemale.domain.chat.service.MessageService; // 메시지 비즈니스 로직 서비스
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor; // 생성자 자동 생성
import org.springframework.http.ResponseEntity; // HTTP 응답 객체
import org.springframework.web.bind.annotation.*; // REST 매핑용 어노테이션
import com.salemale.domain.chat.entity.Message;
import com.salemale.domain.s3.service.S3Service;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/*
 메시지 관련 API 컨트롤러
 - 메시지 전송
 - 메시지 읽음 처리
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Messages")
public class MessageController {

    private final MessageService messageService; // 메시지 서비스
    private final S3Service s3Service;

    /*
     메시지 전송 API
     - 클라이언트에서 채팅방 ID, 내용, 타입(TEXT/IMAGE)을 받아 메시지를 생성한다.
     */
    @Operation(summary = "메시지 보내기", description = "채팅방(chatId)으로 메시지를 보냅니다.")
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @RequestHeader("user_id") Long me,
            @RequestBody SendMessageRequest request
    ) {
        MessageResponse resp = messageService.send(me, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(resp));
    }

    @Operation(summary = "이미지 전송(REST)",
            description = "이미지를 업로드하고, 업로드 URL로 메시지를 저장합니다. 브로드캐스트는 Service에서 처리합니다.")
    @PostMapping(value = "/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageResponse>> sendImage(
            @RequestHeader("user_id") Long me,
            @RequestParam Long chatId,
            @RequestPart("file") MultipartFile file
    ) {
        // 1) 이미지 업로드 → 공개 URL
        String imageUrl = s3Service.uploadToTemp(file);

        // 2) 메시지 저장 (type = IMAGE)
        SendMessageRequest req = SendMessageRequest.builder()
                .chatId(chatId)
                .content(imageUrl)
                .type(Message.MessageType.IMAGE)
                .build();

        MessageResponse saved = messageService.send(me, req);
        return ResponseEntity.ok(ApiResponse.onSuccess(saved));
    }
}
    /*
     (변경 전)메시지 읽음 처리 API
     - 특정 메시지 ID 기준으로, 읽음 상태로 업데이트한다.

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<Void> readMessage(
            @RequestHeader("USER_ID") Long me,
            @PathVariable Long id
    ) {
        messageService.markRead(me, id);
        return ResponseEntity.ok().build();
    }

     */
