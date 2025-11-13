package com.salemale.domain.item.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ImageService {

    // 허용된 이미지 확장자
    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 파일 검증
     * @param file 검증할 파일
     * @throws GeneralException 검증 실패 시
     */
    public void validateFile(MultipartFile file) {
        validateFile(file, MAX_FILE_SIZE, ErrorStatus.IMAGE_SIZE_EXCEEDED);
    }

    /**
     * 파일 검증 (사용자 정의 최대 크기 및 에러 코드)
     * @param file 검증할 파일
     * @param maxFileSize 허용 최대 파일 크기 (bytes)
     * @param sizeExceededError 최대 크기 초과 시 사용할 에러 코드 (null 허용)
     */
    public void validateFile(MultipartFile file, long maxFileSize, ErrorStatus sizeExceededError) {
        // 1. null/empty 체크
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }

        // 2. 파일 크기 체크
        if (file.getSize() > maxFileSize) {
            throw new GeneralException(
                    sizeExceededError != null ? sizeExceededError : ErrorStatus.IMAGE_SIZE_EXCEEDED
            );
        }

        // 3. 확장자 체크
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new GeneralException(ErrorStatus.IMAGE_EXTENSION_INVALID);
        }

        log.debug("파일 검증 성공: {} ({}bytes)", originalFilename, file.getSize());
    }

    /**
     * 파일 확장자 추출
     * @param filename 파일명
     * @return 확장자 (소문자)
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}