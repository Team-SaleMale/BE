package com.salemale.domain.chat.repository;

import com.salemale.domain.chat.entity.Chat; //채팅방 정보
import com.salemale.domain.chat.entity.Message; //메시지 내용
import org.springframework.data.jpa.repository.JpaRepository; // JPA 기본 기능
import java.util.Optional; //null 대신 사용

//메세지 일괄 읽음 처리를 위함
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

//채팅방 입장시 메세지 조회를 위함
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    //채팅방의 최근 메세지 조회
    Optional<Message> findTopByChatOrderBySentAtDesc(Chat chat);
    //목록에 읽지 않은 메세지 카운트
    long countByChat_ChatIdAndSender_IdNotAndIsReadFalse(Long chatId, Long myId);

    // 채팅방의 메시지를 시간 오름차순으로 페이징 조회
    Page<Message> findByChat_ChatIdOrderBySentAtAsc(Long chatId, Pageable pageable);

    // 채팅방의 '상대가 보낸' & '아직 안 읽은' 메시지를 전부 읽음 처리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Message m
           set m.isRead = true
         where m.chat.chatId = :chatId
           and m.isRead = false
           and m.sender.id <> :me
    """)
    int markAllReadInChat(@Param("chatId") Long chatId, @Param("me") Long me);

    // [ADDED] 특정 chatId들의 미읽음 개수 한 번에 조회 (상대가 보낸 & isRead=false)
    @Query("""
        select m.chat.chatId as chatId,
               sum(case when (m.isRead = false and m.sender.id <> :uid) then 1 else 0 end) as unreadCount
          from Message m
         where m.chat.chatId in :chatIds
         group by m.chat.chatId
    """)
    List<Object[]> findUnreadCountsByChatIds(
            @Param("uid") Long uid,
            @Param("chatIds") List<Long> chatIds
    );
}
