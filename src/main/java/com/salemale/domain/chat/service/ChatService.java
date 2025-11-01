package com.salemale.domain.chat.service; // 채팅 비즈니스 로직 계층

import com.salemale.domain.chat.dto.ChatDtos.*; // DTO
import com.salemale.domain.chat.entity.Chat; // 채팅 엔티티
import com.salemale.domain.chat.repository.ChatRepository; // 채팅 리포지토리
import com.salemale.domain.chat.repository.MessageRepository; // 메시지 리포지토리
import com.salemale.domain.item.entity.Item; // 아이템 엔티티
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.user.entity.User; // 유저 엔티티
// import com.salemale.domain.user.repository.UserRepository; // 유저 리포지토리 -> 아이템 참조로 변경
import jakarta.persistence.EntityNotFoundException; // 예외 처리용
import lombok.RequiredArgsConstructor; // 생성자 주입
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/*
 ChatService
 - 채팅방 생성, 조회, 나가기 로직 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository; // 채팅 DB 접근
    private final MessageRepository messageRepository; // 메시지 조회용
    // private final UserRepository userRepository; // 유저 정보 조회
    private final ItemRepository itemRepository; // 상품 정보 조회

    /*
     채팅방 목록 조회
     - 내가 판매자 또는 구매(buyer==winner)로 참여 중인 채팅방 모두 조회
     - 미읽은 메시지 개수(unreadCount) 계산 포함
     */
    public List<ChatSummary> getChatList(Long me, boolean onlyUnread, int page, int size) {
        Page<Chat> chats = chatRepository
                .findBySeller_IdOrBuyer_IdOrderByLastMessageAtDesc(me, me, PageRequest.of(page, size));

        return chats.stream()
                .filter(chat -> {
                    // 내가 나간 방(soft delete) 제외
                    if (chat.getSeller().getId().equals(me)) {
                        return chat.getSellerDeletedAt() == null;
                    } else if (chat.getBuyer().getId().equals(me)) {
                        return chat.getBuyerDeletedAt() == null;
                    }

                    return false;
                })
                .map(chat -> {
                    // 미읽은 메시지 수 계산 (내가 아닌 상대가 보낸 메시지 중 isRead=false)
                    long unread = messageRepository
                            .countByChat_ChatIdAndSender_IdNotAndIsReadFalse(chat.getChatId(), me);
                    // 필터 조건이 unread일 경우, 안 읽은 메시지가 없는 방은 제외
                    if (onlyUnread && unread == 0) return null;
                    return ChatSummary.builder()
                            .chatId(chat.getChatId())
                            .itemId(chat.getItem().getItemId())
                            .sellerId(chat.getSeller().getId())
                            .buyerId(chat.getBuyer().getId())
                            .lastMessageAt(chat.getLastMessageAt())
                            .unreadCount(unread)
                            .build();
                })
                .filter(dto -> dto != null)
                .toList();
    }

    /*
     채팅방 생성(수동)
     - 동일한 (itemId, sellerId, buyerId) 조합 존재 시 재사용
     - 없으면 새 채팅방 생성
     - (추가) 호출자(me)가 seller | winner 인지 권한 검증
     */

    /*
    @Transactional
    public ChatResponse createChat(Long me, CreateChatRequest req) {
        // 1) Item만 조회해서 seller/winner 모두 참조
        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

        User seller = item.getSeller();
        User winner = item.getWinner(); // 낙찰자(없으면 생성 불가)

        if (seller == null) {
            throw new IllegalStateException("판매자 정보가 없습니다.");
        }
        if (winner == null) {
            throw new IllegalStateException("낙찰자가 없습니다.(경매 미완료/유찰)");
        }

        // 호출자 권한 검증 (추가사항)
        if (!seller.getId().equals(me) && !winner.getId().equals(me)) {
            throw new IllegalStateException("채팅방 생성 권한이 없습니다.");
        }


        // 기존 동일 조합이 있는지 확인
        var existing = chatRepository.findByItem_ItemIdAndSeller_IdAndBuyer_Id(
                item.getItemId(), seller.getId(), winner.getId()
        );
        if (existing.isPresent()) {
            return new ChatResponse(existing.get().getChatId());
        }

        // 채팅방 생성 및 저장
        Chat chat = Chat.builder()
                .seller(seller)
                .buyer(winner) //buyer == winner
                .item(item)
                .lastMessageAt(LocalDateTime.now())
                .build();

        Chat saved = chatRepository.save(chat);
        return new ChatResponse(saved.getChatId());
    }

     */

    /*
     채팅방 나가기(삭제)
     - sellerDeletedAt 또는 buyerDeletedAt에 시간 기록
     - 실제 DB 삭제는 하지 않음 (Soft Delete)
     */
    @Transactional
    public void exitChat(Long me, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));
        LocalDateTime now = LocalDateTime.now();

        if (chat.getSeller().getId().equals(me)) {
            chat = Chat.builder()
                    .chatId(chat.getChatId())
                    .seller(chat.getSeller())
                    .buyer(chat.getBuyer())
                    .item(chat.getItem())
                    .lastMessageAt(chat.getLastMessageAt())
                    .sellerDeletedAt(now)
                    .buyerDeletedAt(chat.getBuyerDeletedAt())
                    .build();
        } else if (chat.getBuyer().getId().equals(me)) {
            chat = Chat.builder()
                    .chatId(chat.getChatId())
                    .seller(chat.getSeller())
                    .buyer(chat.getBuyer())
                    .item(chat.getItem())
                    .lastMessageAt(chat.getLastMessageAt())
                    .sellerDeletedAt(chat.getSellerDeletedAt())
                    .buyerDeletedAt(now)
                    .build();
        } else {
            throw new IllegalStateException("참여자가 아닙니다.");
        }
        chatRepository.save(chat);
    }

    // 경매 종료 시 chat 자동 생성용
    @Transactional
    public ChatResponse createChatForItemWinner(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

        User seller = item.getSeller();
        User winner = item.getWinner();

        if (seller == null) {
            throw new IllegalStateException("판매자 정보가 없습니다.");
        }
        if (winner == null) {
            throw new IllegalStateException("낙찰자가 없습니다.(경매 미완료/유찰)");
        }


        var existing = chatRepository.findByItem_ItemIdAndSeller_IdAndBuyer_Id(
                item.getItemId(), seller.getId(), winner.getId()
        );
        if (existing.isPresent()) {
            return new ChatResponse(existing.get().getChatId());
        }

        Chat saved = chatRepository.save(Chat.builder()
                .seller(seller)
                .buyer(winner) // buyer == winner
                .item(item)
                .lastMessageAt(LocalDateTime.now())
                .build());

        return new ChatResponse(saved.getChatId());
    }

}
