package com.salemale.domain.chat.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.chat.dto.MessageDtos;
import com.salemale.domain.chat.dto.MessageDtos.MessageResponse;
import com.salemale.domain.chat.entity.Message;
import com.salemale.domain.chat.service.MessageService;
import com.salemale.domain.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class MessageImageController {

    private final S3Service s3Service;
    private final MessageService messageService;

    @Operation(
            summary = "이미지 전송(REST)",
            description = "이미지를 업로드하고, 메시지를 저장합니다. 브로드캐스트는 Service에서 처리합니다."
    )
    @PostMapping(value = "/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageResponse>> sendImage(
            @RequestHeader("USER_ID") Long me,
            @RequestParam Long chatId,
            @RequestPart("file") MultipartFile file
    ) {
        // 1) 이미지 업로드 → S3 공개 URL
        String imageUrl = s3Service.uploadToTemp(file);

        // 2) 메시지 저장 (type = IMAGE)  Service 내부에서 WS 브로드캐스트 수행
        MessageDtos.SendMessageRequest req = MessageDtos.SendMessageRequest.builder()
                .chatId(chatId)
                .content(imageUrl)
                .type(Message.MessageType.IMAGE)
                .build();

        MessageResponse saved = messageService.send(me, req);

        // 3) 응답 (공통 응답 포맷)
        return ResponseEntity.ok(ApiResponse.onSuccess(saved));
    }
}
