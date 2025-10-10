package com.salemale.domain.item.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.dto.request.ItemRegisterRequest;
import com.salemale.domain.item.dto.response.ItemLikeResponse;
import com.salemale.domain.item.dto.response.ItemRegisterResponse;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemImage;
import com.salemale.domain.item.entity.UserLiked;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.item.repository.UserLikedRepository;
import com.salemale.domain.region.entity.Region;
import com.salemale.domain.region.repository.RegionRepository;
import com.salemale.domain.region.repository.UserRegionRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.global.common.enums.ItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserLikedRepository userLikedRepository;
    private final UserRegionRepository userRegionRepository;
    private final RegionRepository regionRepository;

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
    public ItemRegisterResponse registerItem(String sellerEmail, ItemRegisterRequest request) {

        // 1. 판매자 (User) 조회
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 판매자의 대표 동네 (Region) 조회 -> 테스트 위해 주석 처리
        Region region = userRegionRepository.findByPrimaryUser(seller)
                .map(userRegion -> userRegion.getRegion())
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        // 3. 자동 계산 값 설정 및 시간 처리, 현재 모든 상품의 최소 입찰 금액은 100원 이상 & 시작가의 5% 이상으로 설정
        Integer startPrice = request.getStartPrice();
        Integer bidIncrement = Math.max(100, (int) Math.round(startPrice * 0.05));
        LocalDateTime endTime;
        try {
            // "YYYY-MM-DDTHH:mm" 형식으로 파싱 날짜+시분 까지 입력받음
            endTime = LocalDateTime.parse(request.getEndDateTime());

            // 경매 종료 시간 검증 (현재 시간보다 미래여야 함)
            if (endTime.isBefore(LocalDateTime.now())) {
                throw new GeneralException(ErrorStatus.INVALID_END_TIME); // 또는 새로운 에러 코드
            }

        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST); // 잘못된 날짜/시간 형식 처리
        }

        // 4. Item 엔티티 생성 및 저장
        Item newItem = Item.builder()
                .seller(seller)
                .name(request.getName())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .startPrice(startPrice)
                .currentPrice(startPrice) // 시작가 = 현재가로 초기화
                .bidIncrement(bidIncrement) // 자동 계산된 입찰 단위
                .endTime(endTime)
                .itemStatus(ItemStatus.BIDDING) // 초기 상태는 '입찰중'
                .tradeMethods(request.getTradeMethods())
                .tradeDetails(request.getTradeDetails())
                .region(region)
                .build();

        // 5. ItemImage 엔티티 생성 및 연결 (순서대로 0부터 부여)
        List<ItemImage> images = IntStream.range(0, request.getImageUrls().size())
                .mapToObj(i -> ItemImage.builder()
                        .item(newItem) // Item과의 관계 설정
                        .imageUrl(request.getImageUrls().get(i))
                        .imageOrder(i)
                        .build())
                .toList();

        // OneToMany(cascade = ALL) 설정 덕분에 Item만 저장해도 Image가 함께 저장됩니다.
        newItem.getImages().addAll(images);
        // 6. 저장 및 결과 변수 분리
        Item savedItem = itemRepository.save(newItem); // 재할당되는 변수를 분리

        // 7. Response DTO 반환
        return ItemRegisterResponse.builder()
                .sellerId(seller.getId())
                .itemId(savedItem.getItemId()) // 저장된 엔티티의 ID 사용
                .title(savedItem.getTitle())
                .startPrice(savedItem.getStartPrice())
                .endTime(savedItem.getEndTime())
                .createdAt(savedItem.getCreatedAt())
                .build();
    }
}