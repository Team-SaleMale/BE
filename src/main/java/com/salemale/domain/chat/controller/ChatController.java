package com.salemale.domain.chat.controller; // 채팅 관련 컨트롤러 패키지

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.chat.dto.ChatDtos.*; // 채팅 DTO 묶음 (요청/응답)
import com.salemale.domain.chat.dto.MessageDtos;
import com.salemale.domain.chat.service.ChatService; // 비즈니스 로직 담당 서비스 계층
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor; // 생성자 자동 주입 (final 필드용)
import org.springframework.http.ResponseEntity; // HTTP 응답 객체
import org.springframework.web.bind.annotation.*; // REST API용 어노테이션 (@GetMapping 등)
import java.util.List; // 리스트 응답용
import java.net.URI; //응답 헤더 생성
import com.salemale.domain.chat.dto.ChatDtos.ChatIdUnread;

/**
 채팅방 관련 API 컨트롤러
 - 채팅방 목록 조회
 - 낙찰된 itemId로 채팅 자동 생성/재사용
 - 채팅방 나가기(soft delete)
 */
@RestController
@RequiredArgsConstructor // 생성자 자동 생성 (DI 주입용)
public class ChatController {

    private final ChatService chatService; // 채팅 서비스 의존성

    /**
     채팅방 목록 조회 API
     - 내가 참여 중인 모든 채팅방을 불러오기
     - chatId 리스트 반환
     예) GET /chats?page=0&size=50
     */
    @Operation(summary = "채팅방 목록 조회", description = "chatId와 읽지 않은 메세지 개수 표시.")
    @GetMapping("/chats")
    public ResponseEntity<ApiResponse<List<ChatIdUnread>>> getChats(
           @RequestHeader("user_id") Long me,           // USER가 속한 방들만
           @RequestParam(defaultValue = "0") int page,  // 오프셋 페이징
           @RequestParam(defaultValue = "50") int size
    ) {
        List<ChatIdUnread> result = chatService.getMyChatIds(me, page, size);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
    /**
     낙찰된 itemId만으로 채팅 자동 생성/재사용
     - item.seller / item.winner 를 Chat에 매핑
     - 이미 있으면 기존 chatId 반환
     */
    @Operation(summary = "채팅방 자동 생성", description = "경매 낙찰시 채팅방이 생성됩니다.")
    @PostMapping("/items/{itemId}/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> createChatForWinner(
            @RequestHeader("user_id") Long me,
            @PathVariable Long itemId
    ) {
        ChatResponse resp = chatService.createChatForItemWinner(itemId);
        return ResponseEntity
                .created(URI.create("/chats/" + resp.getChatId()))
                .body(ApiResponse.onSuccess(resp));
    }



    /*
    [구버전]채팅방 생성 API
     - 상품(itemId) 기준으로 구매자/판매자 간 1:1 채팅방 생성
     - 동일한 조합이면 기존 방 재활용 (중복 생성 방지)

    @PostMapping("/chats")
    public ResponseEntity<ChatResponse> createChat(
            @RequestHeader("USER_ID") Long me,
            @RequestBody CreateChatRequest request
    ) {
        return ResponseEntity.ok(chatService.createChat(me, request));
    }

    */

    /**
     채팅방 나가기 API
     - 실제 삭제는 아니고, 각 사용자별 "삭제 시간"만 기록 (soft delete)
     */
    @Operation(summary = "채팅방 나가기", description = "나간 시간이 기록됩니다.")
    @PatchMapping("/chats/{chatId}/exit")
    public ResponseEntity<ApiResponse<Void>> exitChat(
            @RequestHeader("user_id") Long me,
            @PathVariable Long chatId
    ) {
        chatService.exitChat(me, chatId);
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     채팅방 입장 API
     - 읽지 않은 메세지 전체 읽음 처리 + 메시지 오름차순 목록 반환
     - 프론트는 받은 리스트를 그대로 아래로 붙이면 새 메시지가 아래로 쌓임
     */
    @Operation(summary = "채팅방 입장", description = "해당 채팅방의 메시지들이 반환되며 읽지 않은 메시지는 모두 읽음 처리됩니다.")
    @PostMapping("/chats/{chatId}/enter")
    public ResponseEntity<ApiResponse<ChatEnterResponse>> enter(
            @RequestHeader("user_id") Long me,
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        ChatEnterResponse resp = chatService.enter(me, chatId, page, size);
        return ResponseEntity.ok(ApiResponse.onSuccess(resp));
    }

    /**
     메세지 읽음 처리 API
     -채팅방 입장 시: 해당 방의 '내가 안 읽은' 메시지 전체 읽음 처리
     */
    @Operation(summary = "메세지 읽음 처리", description = "채팅방을 입장하지 않고 읽음 처리 기능을 넣을시 유지.")
    @PatchMapping("/chats/{chatId}/read")
    public ResponseEntity<ApiResponse<MessageDtos.ReadAllResponse>> readAllInChat(
            @RequestHeader("user_id") Long me,
            @PathVariable Long chatId
    ) {
        MessageDtos.ReadAllResponse res = chatService.markAllReadInChat(me, chatId);
        return ResponseEntity.ok(ApiResponse.onSuccess(res));
    }
}
