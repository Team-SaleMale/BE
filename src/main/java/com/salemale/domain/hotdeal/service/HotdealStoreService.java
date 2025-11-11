package com.salemale.domain.hotdeal.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.hotdeal.dto.response.HotdealStoreResponse;
import com.salemale.domain.hotdeal.entity.HotdealStore;
import com.salemale.domain.hotdeal.enums.ApprovalStatus;
import com.salemale.domain.hotdeal.repository.HotdealStoreRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 핫딜 가게 정보 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotdealStoreService {

    private final HotdealStoreRepository hotdealStoreRepository;
    private final UserRepository userRepository;

    /**
     * 내 가게 정보 조회
     * @param userId 사용자 ID
     * @return 가게 정보
     */
    public HotdealStoreResponse getMyStore(Long userId) {
        log.info("[핫딜 가게 조회] 사용자 ID: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 사용자 역할 확인
        if (user.getRole() != User.Role.HOTDEAL_VERIFIED) {
            throw new GeneralException(ErrorStatus.HOTDEAL_PERMISSION_DENIED);
        }

        // 3. 승인된 가게 조회
        HotdealStore store = hotdealStoreRepository
                .findByOwnerAndApprovalStatus(user, ApprovalStatus.APPROVED)
                .orElseThrow(() -> new GeneralException(ErrorStatus.HOTDEAL_STORE_NOT_FOUND));

        log.info("[핫딜 가게 조회 완료] 가게 ID: {}, 가게명: {}", store.getStoreId(), store.getStoreName());

        // 4. DTO 변환 및 반환
        return HotdealStoreResponse.from(store);
    }

    /**
     * 핫딜 판매자 권한 검증 (내부 사용)
     * @param userId 사용자 ID
     * @return 승인된 가게 정보
     */
    public HotdealStore validateAndGetStore(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 사용자 역할 확인
        if (user.getRole() != User.Role.HOTDEAL_VERIFIED) {
            log.warn("[핫딜 권한 없음] 사용자 ID: {}, Role: {}", userId, user.getRole());
            throw new GeneralException(ErrorStatus.HOTDEAL_PERMISSION_DENIED);
        }

        // 3. 승인된 가게 조회
        return hotdealStoreRepository
                .findByOwnerAndApprovalStatus(user, ApprovalStatus.APPROVED)
                .orElseThrow(() -> {
                    log.warn("[핫딜 가게 미등록] 사용자 ID: {}", userId);
                    return new GeneralException(ErrorStatus.HOTDEAL_STORE_NOT_FOUND);
                });
    }
}