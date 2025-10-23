package com.salemale.domain.item.converter;

import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.dto.response.LikedItemDTO;
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
            Long bidCount,
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
                .auctionInfo(toAuctionInfo(item, bidCount))
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

    private static AuctionInfoDTO toAuctionInfo(Item item, Long bidCount) {
        return AuctionInfoDTO.builder()
                .startPrice(item.getStartPrice())
                .currentPrice(item.getCurrentPrice())
                .bidIncrement(item.getBidIncrement())
                .endTime(item.getEndTime())
                .bidCount(bidCount)
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

    /**
     * UserLiked Entity → LikedItemDTO 변환
     * @param userLiked 찜한 상품 엔티티
     * @param bidCount 입찰 수
     * @return 찜한 상품 DTO
     */
    public static LikedItemDTO toLikedItemDTO(
            com.salemale.domain.item.entity.UserLiked userLiked,
            Long bidCount
    ) {
        Item item = userLiked.getItem();

        // 썸네일은 첫 번째 이미지 사용
        String thumbnailUrl = item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl();

        return com.salemale.domain.item.dto.response.LikedItemDTO.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .bidderCount(bidCount)
                .endTime(item.getEndTime())
                .viewCount(item.getViewCount())  // 조회수 추가
                .build();
    }

    /**
     * Item Entity → AuctionListItemDTO 변환
     * @param item 경매 상품 엔티티
     * @param bidCount 입찰 수 (실시간 COUNT)
     * @return 경매 상품 리스트 항목 DTO
     */
    public static AuctionListItemDTO toAuctionListItemDTO(
            Item item,
            Long bidCount
    ) {
        // 썸네일은 첫 번째 이미지 사용
        String thumbnailUrl = item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl();

        return com.salemale.domain.item.dto.response.AuctionListItemDTO.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .currentPrice(item.getCurrentPrice())
                .bidderCount(bidCount)
                .endTime(item.getEndTime())
                .viewCount(item.getViewCount())
                .itemStatus(item.getItemStatus().name())
                .build();
    }
}