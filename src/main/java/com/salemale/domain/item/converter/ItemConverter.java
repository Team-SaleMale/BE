package com.salemale.domain.item.converter;

import com.salemale.domain.hotdeal.dto.response.HotdealListItemDTO;
import com.salemale.domain.hotdeal.entity.HotdealStore;
import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.entity.UserLiked;
import com.salemale.domain.mypage.enums.MyRole;
import com.salemale.domain.mypage.dto.response.LikedItemDTO;
import com.salemale.domain.mypage.dto.response.MyAuctionItemDTO;
import com.salemale.domain.item.dto.response.detail.*;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemImage;
import com.salemale.domain.item.entity.ItemTransaction;
import com.salemale.domain.region.dto.response.RegionInfoDTO;
import com.salemale.domain.region.entity.Region;
import com.salemale.domain.user.entity.User;

import java.util.List;
import java.util.stream.Collectors;

public class ItemConverter {

    /**
     * Item Entity → ItemDetailResponse 변환
     */
    public static ItemDetailResponse toItemDetailResponse(
            Item item,
            List<ItemTransaction> bidHistory,
            ItemTransaction highestBid,
            Long likeCount,
            Boolean isLiked
    ) {
        return ItemDetailResponse.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .name(item.getName())
                .description(item.getDescription())
                .category(item.getCategory().name())
                .itemStatus(item.getItemStatus().name())
                .auctionInfo(toAuctionInfo(item))  // bidCount 파라미터 제거
                .sellerInfo(toSellerInfo(item.getSeller()))
                .highestBidder(highestBid != null ? toHighestBidder(highestBid) : null)
                .bidHistory(toBidHistoryList(bidHistory))
                .regionInfo(toRegionInfo(item.getRegion()))
                .tradeInfo(toTradeInfo(item))
                .images(toImageList(item.getImages()))
                .userInteraction(toUserInteraction(isLiked, likeCount))
                .createdAt(item.getCreatedAt())
                .build();
    }

    private static AuctionInfoDTO toAuctionInfo(Item item) {  // bidCount 파라미터 제거
        return AuctionInfoDTO.builder()
                .startPrice(item.getStartPrice())
                .currentPrice(item.getCurrentPrice())
                .bidIncrement(item.getBidIncrement())
                .endTime(item.getEndTime())
                .bidCount(item.getBidCount())  // 엔티티에서 직접 조회
                .build();
    }

    private static SellerInfoDTO toSellerInfo(User seller) {
        return SellerInfoDTO.builder()
                .sellerId(seller.getId())
                .nickname(seller.getNickname())
                .mannerScore(seller.getMannerScore())
                .profileImage(seller.getProfileImage())
                .build();
    }

    private static HighestBidderDTO toHighestBidder(ItemTransaction highestBid) {
        User bidder = highestBid.getBuyer();
        return HighestBidderDTO.builder()
                .userId(bidder.getId())
                .nickname(bidder.getNickname())
                .mannerScore(bidder.getMannerScore())
                .profileImage(bidder.getProfileImage())
                .bidPrice(highestBid.getBidPrice())
                .bidTime(highestBid.getCreatedAt())
                .build();
    }

    private static List<BidHistoryDTO> toBidHistoryList(List<ItemTransaction> transactions) {
        return transactions.stream()
                .map(ItemConverter::toBidHistory)
                .collect(Collectors.toList());
    }

    private static BidHistoryDTO toBidHistory(ItemTransaction transaction) {
        User bidder = transaction.getBuyer();
        return BidHistoryDTO.builder()
                .transactionId(transaction.getTransactionId())
                .bidder(BidHistoryDTO.BidderInfoDTO.builder()
                        .userId(bidder.getId())
                        .nickname(bidder.getNickname())
                        .profileImage(bidder.getProfileImage())
                        .build())
                .bidPrice(transaction.getBidPrice())
                .bidTime(transaction.getCreatedAt())
                .build();
    }

    private static RegionInfoDTO toRegionInfo(Region region) {
        return RegionInfoDTO.builder()
                .sido(region.getSido())
                .sigungu(region.getSigungu())
                .eupmyeondong(region.getEupmyeondong())
                .build();
    }

    private static TradeInfoDTO toTradeInfo(Item item) {
        List<String> methods = item.getTradeMethods().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return TradeInfoDTO.builder()
                .tradeMethods(methods)
                .tradeDetails(item.getTradeDetails())
                .build();
    }

    private static List<ItemImageDTO> toImageList(List<ItemImage> images) {
        return images.stream()
                .map(img -> ItemImageDTO.builder()
                        .imageUrl(img.getImageUrl())
                        .imageOrder(img.getImageOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private static UserInteractionDTO toUserInteraction(Boolean isLiked, Long likeCount) {
        return UserInteractionDTO.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }

    private static String getThumbnailUrl(Item item) {
        return item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl();
    }

    /**
     * UserLiked Entity → LikedItemDTO 변환
     * @param userLiked 찜한 상품 엔티티
     * @return 찜한 상품 DTO
     */
    public static LikedItemDTO toLikedItemDTO(UserLiked userLiked) {
        Item item = userLiked.getItem();

        // 썸네일은 첫 번째 이미지 사용
        String thumbnailUrl = getThumbnailUrl(item);

        return LikedItemDTO.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .bidderCount(item.getBidCount())
                .endTime(item.getEndTime())
                .viewCount(item.getViewCount())  // 조회수 추가
                .build();
    }

    /**
     * Item Entity → AuctionListItemDTO 변환
     * @param item 경매 상품 엔티티
     * @return 경매 상품 리스트 항목 DTO
     */
    public static AuctionListItemDTO toAuctionListItemDTO(Item item) {
        // 전체 이미지 사용
        List<String> imageUrls = item.getImages().stream()
                .map(ItemImage::getImageUrl)
                .collect(Collectors.toList());

        return AuctionListItemDTO.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .imageUrls(imageUrls)
                .currentPrice(item.getCurrentPrice())
                .bidderCount(item.getBidCount())
                .endTime(item.getEndTime())
                .viewCount(item.getViewCount())
                .itemStatus(item.getItemStatus().name())
                .startPrice(item.getStartPrice())  // 시작가
                .createdAt(item.getCreatedAt())    // 시작날짜
                .build();
    }

    /**
     * Item → MyAuctionItemDTO 변환
     * @param item 상품 엔티티
     * @param myRole 사용자 역할 (Service에서 계산된 값)
     * @param isHighestBidder 최고가 입찰자 여부
     * @return MyAuctionItemDTO
     */
    public static MyAuctionItemDTO toMyAuctionItemDTO(
            Item item,
            MyRole myRole,
            Boolean isHighestBidder
    ) {
        String thumbnailUrl = getThumbnailUrl(item);

        return MyAuctionItemDTO.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .viewCount(item.getViewCount())
                .bidderCount(item.getBidCount())
                .startPrice(item.getStartPrice())
                .currentPrice(item.getCurrentPrice())
                .itemStatus(item.getItemStatus().name())
                .endTime(item.getEndTime())
                .myRole(myRole)
                .isHighestBidder(isHighestBidder)
                .build();
    }

    /**
     * Item Entity → HotdealListItemDTO 변환
     * @param item 핫딜 상품 엔티티 (hotdealStore가 fetch join되어 있어야 함)
     * @return 핫딜 리스트 항목 DTO
     */
    public static HotdealListItemDTO toHotdealListItemDTO(Item item) {
        // 이미지 URL 추출
        List<String> imageUrls = item.getImages().stream()
                .map(ItemImage::getImageUrl)
                .collect(Collectors.toList());

        // 핫딜 가게 정보 추출
        HotdealStore store = item.getHotdealStore();

        if (store == null) {
            throw new IllegalArgumentException(
                    "핫딜 상품이 아니거나 가게 정보가 없습니다. itemId: " + item.getItemId()
            );
        }

        return HotdealListItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())              // 상품명
                .imageUrls(imageUrls)
                .currentPrice(item.getCurrentPrice())
                .startPrice(item.getStartPrice())
                .bidderCount(item.getBidCount())
                .endTime(item.getEndTime())
                .itemStatus(item.getItemStatus().name())
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())   // 가게명
                .latitude(store.getLatitude())     // 🔥 위도
                .longitude(store.getLongitude())   // 🔥 경도
                .address(store.getAddress())
                .createdAt(item.getCreatedAt())
                .build();
    }

}