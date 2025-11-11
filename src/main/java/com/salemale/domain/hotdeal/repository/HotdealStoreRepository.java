package com.salemale.domain.hotdeal.repository;

import com.salemale.domain.hotdeal.enums.ApprovalStatus;
import com.salemale.domain.hotdeal.entity.HotdealStore;
import com.salemale.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 핫딜 가게 정보 Repository
 */
@Repository
public interface HotdealStoreRepository extends JpaRepository<HotdealStore, Long> {

    /**
     * 사용자의 승인된 가게 조회
     * @param owner 가게 주인
     * @param status 승인 상태
     * @return 승인된 가게 정보
     */
    Optional<HotdealStore> findByOwnerAndApprovalStatus(User owner, ApprovalStatus status);

    /**
     * 사용자가 승인된 핫딜 판매자인지 확인
     * @param owner 가게 주인
     * @param status 승인 상태
     * @return 승인된 가게 존재 여부
     */
    boolean existsByOwnerAndApprovalStatus(User owner, ApprovalStatus status);

    /**
     * 사용자 ID로 승인된 가게 조회
     * @param userId 사용자 ID
     * @param status 승인 상태
     * @return 승인된 가게 정보
     */
    Optional<HotdealStore> findByOwner_IdAndApprovalStatus(Long userId, ApprovalStatus status);
}
