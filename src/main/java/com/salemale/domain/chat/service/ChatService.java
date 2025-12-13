package com.salemale.domain.chat.service; // 채팅 비즈니스 로직 계층

import com.salemale.domain.chat.dto.BlockResponse;
import com.salemale.domain.chat.dto.ChatDtos.*; // DTO
import com.salemale.domain.chat.dto.MessageDtos;
import com.salemale.domain.chat.dto.BlockStatusResponse; //차단 여부
import com.salemale.domain.chat.entity.Chat; // 채팅 엔티티
import com.salemale.domain.chat.entity.Message; //메시지 엔티티
import com.salemale.domain.chat.repository.ChatRepository; // 채팅 리포지토리
import com.salemale.domain.chat.repository.MessageRepository; // 메시지 리포지토리
import com.salemale.domain.chat.repository.projection.ChatSummaryRow; // 요약 프로젝션
import com.salemale.domain.chat.entity.Message.MessageType; // enum 매핑용
import com.salemale.domain.item.entity.Item; // 아이템 엔티티
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.user.entity.BlockList;
import com.salemale.domain.user.entity.User; // 유저 엔티티
// import com.salemale.domain.user.repository.UserRepository; // 유저 리포지토리 -> 아이템 참조로 변경
import com.salemale.domain.user.repository.BlockListRepository;
import com.salemale.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException; // 예외 처리용
import lombok.RequiredArgsConstructor; // 생성자 주입
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.salemale.domain.alarm.service.AlarmService;                    // 알람용 추가
import com.salemale.domain.alarm.dto.AlarmDtos.CreateAlarmRequest;       // 알람용 추가

/**
 ChatService
 - 채팅방 생성, 조회, 나가기 로직 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository; // 채팅 DB 접근
    private final MessageRepository messageRepository; // 메시지 조회용
    private final UserRepository userRepository; // 유저 정보 조회
    private final ItemRepository itemRepository; // 상품 정보 조회
    private final BlockListRepository blockListRepository;

    private final AlarmService alarmService;   // 알람용 추가

    // 추가 : 채팅 요약 목록 API 서비스 (partner/lastMessage/unreadCount)
    public List<ChatSummaryResponse> getChatSummaries(Long me, int page, int size) {
        int offset = Math.max(page, 0) * Math.max(size, 1);

        List<ChatSummaryRow> rows = chatRepository.findChatSummaries(me, offset, size);

        // 내가 차단한 사용자 id 목록을 한 번에 조회
        List<Long> blockedUserIds = blockListRepository.findBlockedUserIds(me);
        java.util.Set<Long> blockedSet = new java.util.HashSet<>(blockedUserIds);


        return rows.stream().map(r -> {
            // 대화 상대
            ChatSummaryResponse.Partner partner = ChatSummaryResponse.Partner.builder()
                    .id(r.getPartnerId())
                    .nickname(r.getPartnerNickname())
                    .profileImage(r.getPartnerProfileImage())
                    .regionId(r.getPartnerRegionId())
                    .regionName(r.getPartnerRegionName())
                    .build();

            //마지막 메세지
            ChatSummaryResponse.LastMessage last = null;
            if (r.getLastSentAt() != null) {
                MessageType type = null;
                if (r.getLastType() != null) {
                    try {
                        type = MessageType.valueOf(r.getLastType());
                    } catch (IllegalArgumentException ignored) {
                        // 알 수 없는 타입 문자열이면 null로 둠
                    }
                }


                // 메시지 타입에 따라 보여주는 텍스트 가공
                String content = r.getLastContent();
                if (type == MessageType.IMAGE) {
                    content = "사진을 보냈습니다";  // 이미지일 경우 대체 텍스트
                } else if (type == MessageType.URL) {
                    content = "링크를 보냈습니다";  // URL일 경우 대체 텍스트
                }
                last = ChatSummaryResponse.LastMessage.builder()
                        .content(content)
                        .type(type)
                        .sentAt(r.getLastSentAt())
                        .build();
            }

            //  아이템 정보
            ChatSummaryResponse.ItemSummary itemSummary = null;
            if (r.getItemId() != null) { // 쿼리에서 AS itemId 로 넘어오는 값
                itemSummary = ChatSummaryResponse.ItemSummary.builder()
                        .itemId(r.getItemId())
                        .title(r.getItemTitle())      // AS itemTitle
                        .image(r.getItemImageUrl())      // AS itemImage  (대표 이미지 URL)
                        .winningPrice(r.getWinningPrice()) // AS winningPrice (현재는 current_price 사용)
                        .build();
            }

            // 내가 이 대화 상대를 차단했는지 여부
            boolean iBlockedPartner = blockedSet.contains(r.getPartnerId());

            return ChatSummaryResponse.builder()
                    .chatId(r.getChatId())
                    .partner(partner)
                    .lastMessage(last)
                    .unreadCount(r.getUnreadCount() == null ? 0 : r.getUnreadCount())
                    .item(itemSummary)
                    .iBlockedPartner(iBlockedPartner) //차단 여부 추가
                    .build();
        }).toList();
    }


    /**
     * 채팅방 목록 조회
     * USER_ID로 내가 속한 채팅방의 chatId들만 반환(soft delete 제외, 최신 메시지 순)
     * */
    public List<ChatIdUnread> getMyChatIds(Long me, int page, int size) { // [CHANGED] 구현 교체
        // 1) 내 채팅방 id들만 (soft delete 제외 + 최신순) 페이징 조회
        var idPage = chatRepository.findChatIdsByUserOrderByLastMessageAtDesc(
                me, PageRequest.of(page, size)
        );
        List<Long> chatIds = idPage.getContent();
        if (chatIds.isEmpty()) return List.of();

        // 2) 해당 chatId들의 미읽음 개수를 한 번에 집계 (상대가 보낸 & isRead=false)
        //    List<Object[]> findUnreadCountsByChatIds(Long uid, List<Long> chatIds)
        var rows = messageRepository.findUnreadCountsByChatIds(me, chatIds);

        // (chatId -> count) 매핑
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] r : rows) {
            Long chatId = (Long) r[0];
            Long cnt = (r[1] == null) ? 0L : ((Number) r[1]).longValue();
            countMap.put(chatId, cnt);
        }

        // 3) 정렬된 chatIds 순서 유지하며 DTO로 변환
        return chatIds.stream()
                .map(id -> ChatIdUnread.builder()
                        .chatId(id)
                        .unreadCount(countMap.getOrDefault(id, 0L))
                        .build())
                .toList();
    }

    /**
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
                    .sellerDeletedAt(now) //나간 시점 기록
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
                    .buyerDeletedAt(now) //나간 시점 기록
                    .build();
        } else {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
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

    @Transactional
    public ChatEnterResponse enter(Long me, Long chatId, int page, int size) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        // 참여자 검증
        if (!chat.getSeller().getId().equals(me) && !chat.getBuyer().getId().equals(me)) {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
        }

        // 한쪽이라도 나갔으면 입력 비활성
        boolean canSend = (chat.getSellerDeletedAt() == null && chat.getBuyerDeletedAt() == null);

        // 메시지는 오래된→최신 오름차순으로 아래로 쌓이도록
        // 1) 읽지 않은 메세지 일괄 읽음 처리
        int updated = messageRepository.markAllReadInChat(chatId, me);
        int unreadAfter = (int) messageRepository.countByChat_ChatIdAndSender_IdNotAndIsReadFalse(chatId, me);

        // 메시지 조회 (페이징)
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> pageResult = messageRepository.findByChat_ChatIdOrderBySentAtAsc(chatId, pageable);

        List<MessageBrief> messages = pageResult.getContent().stream()
                .map(m -> MessageBrief.builder()
                        .messageId(m.getMessageId())
                        .senderId(m.getSender().getId())
                        .content(m.getContent())
                        .type(m.getType())
                        .read(m.isRead())
                        .sentAt(m.getSentAt())
                        .build())
                .toList();

        return ChatEnterResponse.builder()
                .chatId(chatId)
                .readerId(me)
                .updatedCount(updated)
                .unreadCountAfter(unreadAfter)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .messages(messages)
                .canSend(canSend)
                .build();
    }

    // (변경) 채팅방 단위로 '내가 안 읽은' 메시지 전체 읽음 처리
    @Transactional
    public MessageDtos.ReadAllResponse markAllReadInChat(Long me, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 없습니다."));

        if (!chat.getSeller().getId().equals(me) && !chat.getBuyer().getId().equals(me)) {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
        }

        int updated = messageRepository.markAllReadInChat(chatId, me); // 일괄 업데이트
        int unreadAfter = (int) messageRepository
                .countByChat_ChatIdAndSender_IdNotAndIsReadFalse(chatId, me);

        return MessageDtos.ReadAllResponse.builder()
                .chatId(chatId)
                .readerId(me)
                .updatedCount(updated)
                .unreadCountAfter(unreadAfter)
                .build();
    }

    //대화상대 차단
    @Transactional
    public BlockResponse blockPartner(Long me, Long chatId) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        if (!chat.getSeller().getId().equals(me) && !chat.getBuyer().getId().equals(me)) {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
        }

        Long partnerId = chat.getSeller().getId().equals(me)
                ? chat.getBuyer().getId()
                : chat.getSeller().getId();

        // 이미 차단된 경우
        if (blockListRepository.existsByBlocker_IdAndBlocked_Id(me, partnerId)) {
            return BlockResponse.builder()
                    .blockedUserId(partnerId)
                    .blocked(true)
                    .build();
        }

        User meUser = userRepository.findById(me).orElseThrow();
        User partner = userRepository.findById(partnerId).orElseThrow();

        blockListRepository.save(BlockList.builder()
                .blocker(meUser)
                .blocked(partner)
                .build());

        return BlockResponse.builder()
                .blockedUserId(partnerId)
                .blocked(true)
                .build();
    }

    //대화상대 차단 해제
    @Transactional
    public BlockResponse unblockPartner(Long me, Long chatId) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        // 참여자 검증(코드래빗 피드백)
        if (!chat.getSeller().getId().equals(me) && !chat.getBuyer().getId().equals(me)) {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
        }


        Long partnerId = chat.getSeller().getId().equals(me)
                ? chat.getBuyer().getId()
                : chat.getSeller().getId();

        // 이미 차단 안 된 상태 → blocked=false로 그대로 반환
        if (!blockListRepository.existsByBlocker_IdAndBlocked_Id(me, partnerId)) {
            return BlockResponse.builder()
                    .blockedUserId(partnerId)
                    .blocked(false)
                    .build();
        }

        blockListRepository.deleteByBlocker_IdAndBlocked_Id(me, partnerId);

        return BlockResponse.builder()
                .blockedUserId(partnerId)
                .blocked(false)
                .build();
    }

    @Transactional(readOnly = true)
    public BlockStatusResponse getBlockStatus(Long me, Long chatId) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        if (!chat.getSeller().getId().equals(me) && !chat.getBuyer().getId().equals(me)) {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
        }

        Long partnerId = chat.getSeller().getId().equals(me)
                ? chat.getBuyer().getId()
                : chat.getSeller().getId();

        boolean iBlockedPartner =
                blockListRepository.existsByBlocker_IdAndBlocked_Id(me, partnerId);

        boolean partnerBlockedMe =
                blockListRepository.existsByBlocker_IdAndBlocked_Id(partnerId, me);

        return new BlockStatusResponse(iBlockedPartner, partnerBlockedMe);
    }


}
