package com.salemale.domain.chat.controller; // 채팅 관련 컨트롤러 패키지

import com.salemale.domain.chat.dto.ChatDtos.*; // 채팅 DTO 묶음 (요청/응답)
import com.salemale.domain.chat.service.ChatService; // 비즈니스 로직 담당 서비스 계층
import lombok.RequiredArgsConstructor; // 생성자 자동 주입 (final 필드용)
import org.springframework.http.ResponseEntity; // HTTP 응답 객체
import org.springframework.web.bind.annotation.*; // REST API용 어노테이션 (@GetMapping 등)
import java.util.List; // 리스트 응답용
import java.net.URI; //응답 헤더 생성

/*
 채팅방 관련 API 컨트롤러
 - 채팅방 목록 조회
 - 낙찰된 itemId로 채팅 자동 생성/재사용
 - 채팅방 나가기(soft delete)
 */
@RestController
@RequiredArgsConstructor // 생성자 자동 생성 (DI 주입용)
public class ChatController {

    private final ChatService chatService; // 채팅 서비스 의존성

    /*
     채팅방 목록 조회 API
     - 내가 참여 중인 모든 채팅방을 불러오기
     - 필터(unread), 정렬(sort), 페이징(page, size)
     */
    @GetMapping("/chats")
    public ResponseEntity<List<ChatSummary>> getChats(
            // @AuthenticationPrincipal AuthUser user, // 실제 인증 객체 (테스트 전까지 주석)
            @RequestHeader("USER_ID") Long me, // 임시 사용자 ID (헤더로 전달)
            @RequestParam(required = false) String filter, // 필터: unread 등
            @RequestParam(required = false) String sort, // 정렬 기준
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        boolean onlyUnread = "unread".equalsIgnoreCase(filter); // 필터 조건
        return ResponseEntity.ok(chatService.getChatList(me, onlyUnread, page, size));
    }

    /*
     낙찰된 itemId만으로 채팅 자동 생성/재사용
     - item.seller / item.winner 를 Chat에 매핑
     - 이미 있으면 기존 chatId 반환
     */
    @PostMapping("/items/{itemId}/chat")
    public ResponseEntity<ChatResponse> createChatForWinner(
            @RequestHeader("USER_ID") Long me,
            @PathVariable Long itemId
    ) {
        ChatResponse resp = chatService.createChatForItemWinner(itemId);
        // 201 + Location 헤더로 반환
        return ResponseEntity.created(URI.create("/chats/" + resp.getChatId()))
                .body(resp);
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

    /*
     채팅방 나가기 API
     - 실제 삭제는 아니고, 각 사용자별 "삭제 시간"만 기록 (soft delete)
     */
    @PatchMapping("/chats/{chatId}/exit")
    public ResponseEntity<Void> exitChat(
            @RequestHeader("USER_ID") Long me,
            @PathVariable Long chatId
    ) {
        chatService.exitChat(me, chatId);
        return ResponseEntity.ok().build();
    }
}
