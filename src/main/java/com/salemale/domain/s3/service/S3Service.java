package com.salemale.domain.s3.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * temp 폴더에 이미지 업로드
     * @param file 업로드할 파일
     * @return S3 공개 URL
     */
    public String uploadToTemp(MultipartFile file) {

        // 2. 파일명 생성 (UUID_원본파일명)
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID() + "_" + originalFilename;

        // 3. S3 키 생성 (temp/UUID_파일명.jpg)
        String s3Key = "temp/" + fileName;

        // 4. S3 업로드
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            // 스트리밍 방식으로 변경 (메모리 절약)
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            // 5. 공개 URL 생성
            return generatePublicUrl(s3Key);

        } catch (S3Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        } catch (IOException e) {
            log.error("파일 읽기 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * temp 폴더의 이미지를 items 폴더로 이동
     * @param tempUrl temp 폴더의 이미지 URL
     * @return 새로운 items 폴더의 URL
     */
    public String moveToItems(String tempUrl) {
        // 1. URL에서 S3 키 추출
        String tempKey = extractS3KeyFromUrl(tempUrl);

        // 2. temp 키인지 검증
        if (!tempKey.startsWith("temp/")) {
            log.warn("temp URL이 아닙니다: {}", tempUrl);
            return tempUrl; // 이미 items 폴더에 있는 경우 그대로 반환
        }

        // 3. 파일명 추출
        String fileName = tempKey.substring("temp/".length());

        // 4. 새로운 키 생성 (items/2025/01/UUID_파일명.jpg)
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String newKey = "items/" + yearMonth + "/" + fileName;

        try {
            // 5. S3 복사 (temp → items)
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(tempKey)
                    .destinationBucket(bucketName)
                    .destinationKey(newKey)
                    .build();

            s3Client.copyObject(copyObjectRequest);

            // 복사 성공 여부 검증 (추가)
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newKey)
                    .build();

            s3Client.headObject(headRequest); // 파일이 없으면 예외 발생
            log.info("S3 파일 복사 검증 완료: {}", newKey);

            // 6. temp 파일 삭제 (복사 검증 후에만 삭제)
            deleteFile(tempKey);

            // 7. 새 URL 반환
            return generatePublicUrl(newKey);

        } catch (S3Exception e) {
            log.error("S3 파일 이동 실패: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에서 파일 삭제
     * @param s3Key 삭제할 파일의 S3 키
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", s3Key);

        } catch (S3Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
            // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * S3 공개 URL 생성
     * @param s3Key S3 키
     * @return 공개 URL
     */
    private String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }

    /**
     * URL에서 S3 키 추출
     * @param url S3 URL
     * @return S3 키
     */
    private String extractS3KeyFromUrl(String url) {
        // https://bucket-name.s3.region.amazonaws.com/temp/uuid_file.jpg
        // → temp/uuid_file.jpg
        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            // URL 디코딩 처리
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);

            // 앞의 슬래시 제거
            return decodedPath.startsWith("/") ? decodedPath.substring(1) : decodedPath;

        } catch (URISyntaxException e) {
            log.error("잘못된 URL 형식: {}", url);
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_URL);
        }
    }
}