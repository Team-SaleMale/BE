package com.salemale.domain.chat.repository;

import com.salemale.domain.chat.entity.Chat; //채팅방 정보
import org.springframework.data.domain.Page; // 페이징
import org.springframework.data.domain.Pageable; // 페이지 번호, 크기, 정렬 정보가 들어있는 객체
import org.springframework.data.jpa.repository.JpaRepository; // JPA 기본 기능
import java.util.Optional; //null 대신 사용

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // 아이템/판매자/구매자 조합으로 중복 생성 방지
    Optional<Chat> findByItem_ItemIdAndSeller_IdAndBuyer_Id(Long itemId, Long sellerId, Long buyerId);

    //USER_ID가 속한(soft delete 제외) 채팅방의 chatId와 읽지 않은 메시지의 개수만, 최근 대화 순서로 반환
    @Query("""
    select c.chatId
      from Chat c
     where (c.seller.id = :uid and c.sellerDeletedAt is null)
        or (c.buyer.id  = :uid and c.buyerDeletedAt  is null)
     order by c.lastMessageAt desc
""")
    Page<Long> findChatIdsByUserOrderByLastMessageAtDesc(@Param("uid") Long uid, Pageable pageable);
    /*
    Page<ChatIdUnreadProjection> findChatIdsWithUnreadByUser(
            @Param("uid") Long uid,
            Pageable pageable
    );
     */
}
