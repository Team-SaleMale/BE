package com.salemale.domain.item.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    // 업로드된 이미지들의 임시 URL 리스트
    private List<String> imageUrls;

    // 업로드된 이미지 개수
    private Integer count;

    public static ImageUploadResponse of(List<String> imageUrls) {
        return ImageUploadResponse.builder()
                .imageUrls(imageUrls)
                .count(imageUrls.size())
                .build();
    }
}