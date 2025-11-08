package com.salemale.domain.mypage.dto.response;

import com.salemale.global.common.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "선호 카테고리 응답")
public class PreferredCategoryResponse {

    @Schema(description = "선호 카테고리 목록", example = "[\"SPORTS\", \"PLANT\", \"TICKET\"]")
    private List<Category> categories;

    @Schema(description = "총 선택된 카테고리 개수", example = "3")
    private int count;
}