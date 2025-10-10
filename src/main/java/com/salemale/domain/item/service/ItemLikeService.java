package com.salemale.domain.item.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.dto.response.ItemLikeResponse;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.UserLiked;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.item.repository.UserLikedRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemLikeService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserLikedRepository userLikedRepository;

    @Transactional
    public ItemLikeResponse likeItem(String email, Long itemId) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_FOUND));

        // 3. 본인 상품 찜하기 방지
        if (item.getSeller().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus.ITEM_SELF_LIKE_FORBIDDEN);
        }

        // 4. 이미 찜했는지 확인
        if (userLikedRepository.existsByUserAndItem(user, item)) {
            throw new GeneralException(ErrorStatus.ITEM_ALREADY_LIKED);
        }

        // 5. 찜하기 생성
        UserLiked userLiked = UserLiked.builder()
                .user(user)
                .item(item)
                .liked(true)
                .build();
        userLikedRepository.save(userLiked);

        // 6. DTO로 응답 반환
        return ItemLikeResponse.of(itemId, true);
    }

    @Transactional
    public ItemLikeResponse unlikeItem(String email, Long itemId) {

        // 1. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_FOUND));

        // 3. 찜한 레코드 찾기
        UserLiked userLiked = userLikedRepository.findByUserAndItem(user, item)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_LIKED));

        // 4. 찜 취소 (삭제)
        userLikedRepository.delete(userLiked);

        // 5. 응답 반환
        return ItemLikeResponse.of(itemId, false);
    }
}