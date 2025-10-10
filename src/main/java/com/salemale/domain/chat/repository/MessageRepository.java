package com.salemale.domain.chat.repository;

import com.salemale.domain.chat.entity.Chat; //채팅방 정보
import com.salemale.domain.chat.entity.Message; //메시지 내용
import org.springframework.data.jpa.repository.JpaRepository; // JPA 기본 기능
import java.util.Optional; //null 대신 사용

public interface MessageRepository extends JpaRepository<Message, Long> {
    //채팅방의 최근 메세지 조회
    Optional<Message> findTopByChatOrderBySentAtDesc(Chat chat);
    //목록에 읽지 않은 메세지 카운트
    long countByChat_ChatIdAndSender_IdNotAndIsReadFalse(Long chatId, Long myId);
}
