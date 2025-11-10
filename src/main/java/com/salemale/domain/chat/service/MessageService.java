package com.salemale.domain.chat.service; // 메시지 관련 비즈니스 로직 계층

import com.salemale.domain.chat.dto.MessageDtos.*; // 메시지 요청/응답 DTO
import com.salemale.domain.chat.entity.Chat; // 채팅 엔티티
import com.salemale.domain.chat.entity.Message; // 메시지 엔티티
import com.salemale.domain.chat.repository.ChatRepository; // 채팅 리포지토리
import com.salemale.domain.chat.repository.MessageRepository; // 메시지 리포지토리
import com.salemale.domain.user.entity.User; // 유저 엔티티
import com.salemale.domain.user.repository.UserRepository; // 유저 리포지토리
import jakarta.persistence.EntityNotFoundException; // 예외처리용
import lombok.RequiredArgsConstructor; // 생성자 자동 주입
import org.springframework.stereotype.Service; // 서비스 빈 등록
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 관리
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime; // 시간 기록용
import lombok.extern.slf4j.Slf4j; //로깅용
import org.springframework.messaging.simp.SimpMessagingTemplate; // WS 브로드캐스트용

/*
 MessageService
 - 메시지 전송, 읽음 처리 로직 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    //브로드캐스트는 이벤트로 위임(템플릿 의존 제거)
    private final ApplicationEventPublisher eventPublisher;

    /*
     메시지 전송
     - 본인이 채팅 참여자(seller/buyer)인지 확인
     - soft delete된 방이면 전송 불가
     */
    @Transactional
    public MessageResponse send(Long me, SendMessageRequest req) {
        Chat chat = chatRepository.findById(req.getChatId())
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        // 참여자 검증
        if (!chat.getSeller().getId().equals(me) && !chat.getBuyer().getId().equals(me)) {
            throw new IllegalStateException("참여자가 아닙니다.");
        }

        // 어느 한쪽이라도 나가면 전체 전송 차단
        if (chat.getSellerDeletedAt() != null || chat.getBuyerDeletedAt() != null) {
            throw new IllegalStateException("이 대화는 종료되었습니다.");
        }

        User sender = userRepository.findById(me)
                .orElseThrow(() -> new EntityNotFoundException("보내는 사용자가 없습니다."));

        // 메시지 생성
        Message msg = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(req.getContent())
                .type(req.getType())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .isDeleted(false)
                .build();

        // 저장
        Message saved = messageRepository.save(msg);
/*
        //(기존)채팅방의 마지막 대화 시간 갱신 -> Chat.builder로 새 객체 생성
        chat = Chat.builder()
                .chatId(chat.getChatId())
                .seller(chat.getSeller())
                .buyer(chat.getBuyer())
                .item(chat.getItem())
                .lastMessageAt(saved.getSentAt())
                .sellerDeletedAt(chat.getSellerDeletedAt())
                .buyerDeletedAt(chat.getBuyerDeletedAt())
                .build();
        chatRepository.save(chat);

 */
        // 변경: 기존 엔티티 필드 업데이트
        chat.updateLastMessageAt(saved.getSentAt());
        chatRepository.save(chat);

        // 응답 DTO 생성
        MessageResponse dto = MessageResponse.builder()
                .messageId(saved.getMessageId())
                .chatId(chat.getChatId())
                .senderId(sender.getId())
                .content(saved.getContent())
                .type(saved.getType())
                .read(saved.isRead())
                .sentAt(saved.getSentAt())
                .build();

        // 저장 완료 이벤트 발행 → Listener에서 WS 브로드캐스트
        eventPublisher.publishEvent(new MessageSentEvent(dto)); // 새 이벤트 클래스

        return dto;

    }
}
