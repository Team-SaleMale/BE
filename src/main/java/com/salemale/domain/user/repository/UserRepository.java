package com.salemale.domain.user.repository; // User 엔티티용 JPA 리포지토리(프로필 CRUD 담당)

import com.salemale.domain.user.entity.User; // 도메인 엔티티
import org.springframework.data.jpa.repository.JpaRepository; // 스프링 데이터 JPA 리포지토리

public interface UserRepository extends JpaRepository<User, Long> {
    // 필요 시 사용자 정의 조회 메서드 추가 예정
}


