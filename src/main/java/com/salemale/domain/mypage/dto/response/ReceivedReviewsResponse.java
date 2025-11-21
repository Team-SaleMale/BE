package com.salemale.domain.mypage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 받은 후기 목록 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ReceivedReviewsResponse {

    // 후기 목록
    private List<ReceivedReviewDTO> reviews;

    // 페이징 정보
    private Long totalElements;          // 전체 후기 수
    private Integer totalPages;          // 전체 페이지 수
    private Integer currentPage;         // 현재 페이지 번호
    private Integer size;                // 페이지당 아이템 수
    private Boolean hasNext;             // 다음 페이지 존재 여부
    private Boolean hasPrevious;         // 이전 페이지 존재 여부
}