package com.salemale.domain.chat.repository;

import com.salemale.domain.chat.entity.Chat; //채팅방 정보
import org.springframework.data.domain.Page; // 페이징
import org.springframework.data.domain.Pageable; // 페이지 번호, 크기, 정렬 정보가 들어있는 객체
import org.springframework.data.jpa.repository.JpaRepository; // JPA 기본 기능

import java.util.List;
import java.util.Optional; //null 대신 사용
import com.salemale.domain.chat.repository.projection.ChatSummaryRow;

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

    // 추가: 요약 목록 (partner/lastMessage/unreadCount)
    @Query(value = """
        SELECT
              c.chat_id           AS chatId
            , CASE WHEN :me = c.seller_id THEN c.buyer_id ELSE c.seller_id END AS partnerId
            , u.nickname          AS partnerNickname
            , u.profile_image     AS partnerProfileImage
            , lm.content          AS lastContent
            , lm.type             AS lastType
            , lm.sent_at          AS lastSentAt
            , (
                SELECT COUNT(*)
                FROM message mm
                WHERE mm.chat_id = c.chat_id
                  AND mm.is_read = false
                  AND mm.sender_id <> :me
              )                   AS unreadCount
                
            -- 아이템 요약 정보
            , i.item_id           AS itemId
            , i.title             AS itemTitle
            , ii.image_url        AS itemImage
            , i.current_price     AS winningPrice
                
        FROM chat c
                    
            -- [ADD] item 조인
        JOIN item i
          ON i.item_id = c.item_id
                    
        JOIN users u
          ON u.id = CASE WHEN :me = c.seller_id THEN c.buyer_id ELSE c.seller_id END
        LEFT JOIN LATERAL (
            SELECT content, type, sent_at
            FROM message m
            WHERE m.chat_id = c.chat_id
            ORDER BY m.sent_at DESC
            LIMIT 1
        ) lm ON TRUE
                     
        -- 대표 이미지 한 장만 가져오는 LATERAL 조인
        LEFT JOIN LATERAL (
            SELECT image_url
            FROM item_image ii2
            WHERE ii2.item_id = i.item_id
            ORDER BY ii2.image_order ASC
            LIMIT 1
        ) ii ON TRUE
                     
        WHERE (:me = c.seller_id OR :me = c.buyer_id)
          AND (
               (:me = c.seller_id AND c.seller_deleted_at IS NULL)
            OR (:me = c.buyer_id  AND c.buyer_deleted_at  IS NULL)
          )
        ORDER BY COALESCE(lm.sent_at, c.created_at) DESC
        OFFSET :offset LIMIT :limit
        """, nativeQuery = true)
    List<ChatSummaryRow> findChatSummaries(
            @Param("me") Long me,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
