package com.salemale.domain.user.service;

import com.salemale.domain.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileImageCleanupService {

    private final S3Service s3Service;

    @Async("profileImageCleanupExecutor")
    public void deleteProfileImageAsync(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            s3Service.deleteFileByUrl(imageUrl);
            log.info("프로필 이미지 삭제 완료: {}", imageUrl);
        } catch (Exception ex) {
            log.error("프로필 이미지 삭제 실패: {}", imageUrl, ex);
        }
    }
}


