package com.salemale.domain.chat.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.chat.dto.MessageDtos;
import com.salemale.domain.chat.dto.MessageDtos.MessageResponse;
import com.salemale.domain.chat.service.MessageService;
import com.salemale.domain.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class MessageImageController {

    private final S3Service s3Service;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "이미지 전송(REST)", description = "이미지를 업로드하고, 업로드 완료 메시지를 WS로 알립니다.")
    @PostMapping(value = "/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageResponse>> sendImage(
            @RequestHeader("USER_ID") Long me,
            @RequestParam Long chatId,
            @RequestPart("file") MultipartFile file
    ) {
        // 1) 이미지 업로드 → S3 공개 URL
        String imageUrl = s3Service.uploadToTemp(file);  // temp 업로드 (원하면 바로 items로도 가능)

        // 2) 메시지 저장 (type=IMAGE)
        MessageDtos.SendMessageRequest req = MessageDtos.SendMessageRequest.builder()
                .chatId(chatId)
                .content(imageUrl)
                .type(MessageType.IMAGE)
                .build();

        MessageResponse saved = messageService.send(me, req);

        // 3) WS 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chats/" + chatId, saved);

        return ResponseEntity.ok(ApiResponse.<MessageResponse>onSuccess(saved));
    }
}
