package com.salemale.domain.user.entity; // 사용자 차단 관계 엔티티(한 사용자가 다른 사용자를 차단)

import com.salemale.global.common.BaseEntity; // 생성/수정 시간 등 공통 컬럼 상속
import jakarta.persistence.*; // JPA 매핑 애노테이션
import lombok.AccessLevel; // 생성자 접근 제한자
import lombok.AllArgsConstructor; // 전체 필드 생성자
import lombok.Builder; // 빌더 패턴
import lombok.Getter; // 게터 생성
import lombok.NoArgsConstructor; // 기본 생성자

@Entity // JPA 엔티티
@Table(name = "block_list") // 차단 목록 테이블
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BlockList extends BaseEntity {

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDENTITY 전략
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 차단을 하는 사용자
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY) // 차단된 사용자
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blocked;
}
