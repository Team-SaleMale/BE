package com.salemale.domain.user.repository; // User 엔티티용 JPA 리포지토리(프로필 CRUD 담당)

import com.salemale.domain.user.entity.User; // 도메인 엔티티
import org.springframework.data.jpa.repository.JpaRepository; // 스프링 데이터 JPA 리포지토리

public interface UserRepository extends JpaRepository<User, Long> {
    // 닉네임이 이미 존재하는지 여부를 빠르게 판단하기 위한 existsBy 쿼리 메서드
    boolean existsByNickname(String nickname);

    // 이메일로 사용자 조회 — JWT subject가 이메일인 경우 id 매핑용
    java.util.Optional<User> findByEmail(String email);
}


