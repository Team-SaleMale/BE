package com.salemale.domain.mypage.dto.request;

import com.salemale.global.common.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "선호 카테고리 설정 요청")
public class UpdatePreferredCategoryRequest {

    @NotEmpty(message = "최소 1개 이상의 카테고리를 선택해야 합니다.")
    @Size(max = 17, message = "최대 17개까지 선택 가능합니다.")
    @Schema(description = "선호 카테고리 목록", example = "[\"SPORTS\", \"PLANT\", \"TICKET\"]")
    private List<Category> categories;
}