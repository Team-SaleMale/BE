package com.salemale.domain.chat.repository;

import com.salemale.domain.chat.entity.Chat; //채팅방 정보
import org.springframework.data.domain.Page; // 여러 개의 결과를 한 페이지 단위로 가져올 때 사용
import org.springframework.data.domain.Pageable; // 페이지 번호, 크기, 정렬 정보가 들어있는 객체
import org.springframework.data.jpa.repository.JpaRepository; // JPA 기본 기능
import java.util.Optional; //null 대신 사용

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // 내 채팅방들(판매자거나 구매자인 방)
    Page<Chat> findBySeller_IdOrBuyer_IdOrderByLastMessageAtDesc(Long sellerId, Long buyerId, Pageable pageable);

    // 아이템/판매자/구매자 조합으로 중복 생성 방지
    Optional<Chat> findByItem_ItemIdAndSeller_IdAndBuyer_Id(Long itemId, Long sellerId, Long buyerId);
}
