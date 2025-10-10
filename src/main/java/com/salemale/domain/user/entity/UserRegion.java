package com.salemale.domain.user.entity; // 사용자-지역 연결 엔티티: 사용자가 활동하는 동네 정보를 관리합니다.

import com.salemale.domain.region.entity.Region; // 지역 엔티티(시군구/읍면동 정보를 담고 있음)
import com.salemale.global.common.BaseEntity; // 생성/수정 시간 등 공통 필드를 제공하는 부모 엔티티
import jakarta.persistence.*; // JPA 매핑 애노테이션 패키지 전반 사용(@Entity, @ManyToOne 등)
import lombok.*; // Lombok 애노테이션으로 보일러플레이트 코드 자동 생성

/**
 * UserRegion: 사용자와 지역(동네)의 연결 관계를 나타내는 엔티티입니다.
 *
 * - 한 사용자는 여러 동네를 등록할 수 있습니다(예: 집 동네, 회사 동네).
 * - isPrimary 플래그를 통해 "주 활동 동네"를 구분합니다.
 * - 이 정보는 사용자가 보는 게시물/아이템의 지역 필터링에 사용됩니다.
 *
 * 실사용 예:
 * - 사용자가 앱에서 "동네 추가"를 하면 UserRegion 레코드가 생성됩니다.
 * - isPrimary=true인 동네가 기본 화면에 표시되는 동네가 됩니다.
 */
@Entity // JPA 엔티티로 선언: 데이터베이스 테이블과 1:1 매핑됩니다.
@Table(name = "user_region") // 테이블명 지정: user_region 테이블로 매핑
@Getter // Lombok: 모든 필드에 대한 getter 메서드 자동 생성
@Builder // Lombok: 빌더 패턴 자동 생성으로 가독성 높은 객체 생성 가능
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: JPA가 필요로 하는 기본 생성자를 protected로 생성
@AllArgsConstructor // Lombok: 모든 필드를 받는 생성자 자동 생성(빌더와 함께 사용)
public class UserRegion extends BaseEntity { // BaseEntity를 상속하여 createdAt, updatedAt 등의 공통 필드를 자동으로 가짐

    @Id // 기본 키(Primary Key) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 데이터베이스의 AUTO_INCREMENT 기능을 사용하여 자동으로 ID 생성
    private Long id; // 사용자-지역 연결의 고유 식별자

    // 사용자(User)와의 다대일(Many-to-One) 관계: 한 사용자는 여러 UserRegion을 가질 수 있음
    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩: UserRegion 조회 시 User는 실제 사용할 때만 DB에서 가져옴(성능 최적화)
    @JoinColumn(name = "user_id", nullable = false) // 외래 키 컬럼명 지정: user_id, NULL 불가(반드시 사용자와 연결되어야 함)
    private User user; // 이 동네 정보를 소유한 사용자

    // 지역(Region)과의 다대일(Many-to-One) 관계: 한 지역은 여러 UserRegion에 연결될 수 있음
    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩: UserRegion 조회 시 Region은 실제 사용할 때만 DB에서 가져옴
    @JoinColumn(name = "region_id", nullable = false) // 외래 키 컬럼명 지정: region_id, NULL 불가(반드시 지역과 연결되어야 함)
    private Region region; // 사용자가 활동하는 구체적인 동네(행정구역)

    // isPrimary: 사용자의 "주 활동 동네"인지 여부를 나타내는 플래그
    // - true: 이 동네가 사용자의 메인 동네(앱 첫 화면에 표시)
    // - false: 서브 동네(필요할 때 선택 가능)
    @Builder.Default // 빌더 사용 시에도 기본값 false가 유지되도록 지정
    @Column(name = "is_primary", nullable = false) // 컬럼명 지정: is_primary, NULL 불가(반드시 true/false 값 필요)
    private boolean isPrimary = false; // 기본값: 주 활동 동네 아님
}


