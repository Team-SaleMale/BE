package com.salemale.domain.user.repository;

import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.entity.UserPreferredCategory;
import com.salemale.global.common.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferredCategoryRepository extends JpaRepository<UserPreferredCategory, Long> {

    // 사용자의 모든 선호 카테고리 조회
    List<UserPreferredCategory> findByUser(User user);

    // 사용자의 선호 카테고리 전체 삭제
    void deleteByUser(User user);

    // 사용자의 특정 카테고리 존재 여부 확인
    boolean existsByUserAndCategory(User user, Category category);
}