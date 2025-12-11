package com.salemale.domain.chat.service; // 메시지 관련 비즈니스 로직 계층

import com.salemale.domain.chat.dto.MessageDtos.*; // 메시지 요청/응답 DTO
import com.salemale.domain.chat.entity.Chat; // 채팅 엔티티
import com.salemale.domain.chat.entity.Message; // 메시지 엔티티
import com.salemale.domain.chat.repository.ChatRepository; // 채팅 리포지토리
import com.salemale.domain.chat.repository.MessageRepository; // 메시지 리포지토리
import com.salemale.domain.user.entity.User; // 유저 엔티티
import com.salemale.domain.user.repository.UserRepository; // 유저 리포지토리
import com.salemale.domain.user.repository.BlockListRepository; // 차단
import jakarta.persistence.EntityNotFoundException; // 예외처리용
import lombok.RequiredArgsConstructor; // 생성자 자동 주입
import org.springframework.stereotype.Service; // 서비스 빈 등록
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 관리
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime; // 시간 기록용
import lombok.extern.slf4j.Slf4j; //로깅용
import org.springframework.messaging.simp.SimpMessagingTemplate; // WS 브로드캐스트용

import com.salemale.domain.alarm.service.AlarmService;                    // 알람용 추가
import com.salemale.domain.alarm.dto.AlarmDtos.CreateAlarmRequest;       // 알람용 추가

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
    private final BlockListRepository blockListRepository;

    //브로드캐스트는 이벤트로 위임(템플릿 의존 제거)
    private final ApplicationEventPublisher eventPublisher;
    private final AlarmService alarmService;   // 알람용 추가

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

        // 새 메시지 알림: 상대방에게만 전송
        Long sellerId = chat.getSeller().getId();
        Long buyerId  = chat.getBuyer().getId();
        Long senderId = me;

        // 내가 판매자면 수신자는 구매자, 내가 구매자면 수신자는 판매자
        Long receiverId = senderId.equals(sellerId) ? buyerId : sellerId;

        // 내가 상대를 차단한 경우 → 메시지 저장 X
        if (blockListRepository.existsByBlocker_IdAndBlocked_Id(me, receiverId)) {
            return MessageResponse.builder()
                    .ignored(true)     // 무시하기
                    .build();
        }

        // 상대가 나를 차단한 경우 → 메시지 저장 X
        if (blockListRepository.existsByBlocker_IdAndBlocked_Id(receiverId, me)) {
            return MessageResponse.builder()
                    .ignored(true)     // 무시하기
                    .build();
        }

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

        // 변경: 기존 엔티티 필드 업데이트
        chat.updateLastMessageAt(saved.getSentAt());
        chatRepository.save(chat);


        // 발신자 = 수신자 케이스는 방어
        if (!receiverId.equals(senderId)) {
            String content = saved.getContent() == null ? "" : saved.getContent();
            String preview = content.length() > 10 ? content.substring(0, 10) + "..." : content;

            String msgForReceiver = "새 메시지가 도착했습니다: " + preview;
            alarmService.createAlarm(new CreateAlarmRequest(receiverId, msgForReceiver));
        }

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
