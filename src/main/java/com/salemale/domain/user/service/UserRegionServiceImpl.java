package com.salemale.domain.user.service; // 사용자-지역 할당 비즈니스 로직 구현체

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.region.entity.Region; // 지역 엔티티(시군구/읍면동 정보)
import com.salemale.domain.user.entity.UserRegion; // 사용자-지역 연결 엔티티
import com.salemale.domain.region.repository.RegionRepository; // 지역 저장소
import com.salemale.domain.user.entity.User; // 사용자 엔티티
import com.salemale.domain.user.repository.UserRegionRepository; // 사용자-지역 연결 저장소
import com.salemale.domain.user.repository.UserRepository; // 사용자 저장소
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import org.springframework.stereotype.Service; // 스프링 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리

/**
 * UserRegionServiceImpl: 사용자의 활동 동네를 관리하는 서비스 구현체입니다.
 *
 * - UserRegionService 인터페이스를 구현합니다.
 * - 사용자에게 동네를 할당하고 변경하는 로직을 처리합니다.
 * - 현재는 사용자당 1개의 동네만 유지하는 정책을 적용합니다.
 *
 * 동작 원리:
 * 1. 지역 검색은 SearchController에서 수행합니다.
 * 2. 프론트엔드에서 선택한 지역 ID를 받아 사용자에게 할당합니다.
 * 3. 기존 UserRegion을 모두 삭제하고 새로운 연결을 생성합니다(1:1 제약).
 */
@Service // 스프링이 이 클래스를 서비스 빈으로 등록하여 컨트롤러에서 주입받을 수 있게 합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
public class UserRegionServiceImpl implements UserRegionService { // UserRegionService 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final UserRepository userRepository; // 사용자 정보 조회/저장
    private final RegionRepository regionRepository; // 지역 정보 조회
    private final UserRegionRepository userRegionRepository; // 사용자-지역 연결 정보 조회/저장/삭제

    /**
     * 선택된 지역 ID로 사용자의 동네를 직접 설정합니다.
     *
     * - 2단계 프로세스(검색 → 선택)에서 사용됩니다.
     * - 사용자가 여러 검색 결과 중 하나를 선택한 경우 이 메서드를 호출합니다.
     * - 사용자의 기존 동네를 해제하고 새 동네를 설정합니다.
     *
     * @param userId 동네를 설정할 사용자 ID
     * @param regionId 설정할 지역 ID
     * @param primary 주 활동 동네 여부(현재는 항상 true)
     * @return 설정된 지역(Region)의 ID
     * @throws GeneralException 사용자를 찾을 수 없거나 지역을 찾을 수 없을 때 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 데이터베이스 변경 작업을 하나의 트랜잭션으로 묶어 일관성을 보장합니다.
    public Long setRegionForUser(Long userId, Long regionId, boolean primary) {
        // 1) 사용자 존재 확인: 주어진 ID로 사용자를 조회합니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2) 지역 존재 확인: 주어진 ID로 지역을 조회합니다.
        Region target = regionRepository.findById(regionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_FOUND));

        // 3) 동네 할당 적용: 기존 동네를 해제하고 새 동네를 설정합니다.
        applyAssignment(user, target, primary);

        // 4) 설정된 지역 ID 반환
        return target.getRegionId();
    }

    /**
     * 내부 메서드: 사용자에게 지역을 할당하는 실제 로직을 처리합니다.
     *
     * - 1:1 제약을 유지하기 위해 기존의 모든 UserRegion을 삭제합니다.
     * - 새로운 UserRegion을 생성하여 저장합니다.
     * - isPrimary는 항상 true로 설정됩니다(단일 관계이므로).
     *
     * @param user 동네를 할당받을 사용자
     * @param target 할당할 지역
     * @param primary 주 활동 동네 여부(현재는 사용되지 않지만 확장성을 위해 유지)
     */
    private void applyAssignment(User user, Region target, boolean primary) {
        // 1) 기존 동네 삭제: 1:1 제약을 만족시키기 위해 사용자의 모든 기존 UserRegion을 제거합니다.
        //    - findAllByUser: 해당 사용자의 모든 UserRegion을 조회합니다.
        //    - forEach: 각 UserRegion을 순회하며 삭제합니다.
        //    - deleteById: ID를 기준으로 레코드를 삭제합니다.
        userRegionRepository.findAllByUser(user).forEach(ur -> userRegionRepository.deleteById(ur.getId()));

        // 2) 새 동네 생성: UserRegion 엔티티를 빌더 패턴으로 생성합니다.
        //    - user: 동네를 할당받을 사용자
        //    - region: 할당할 지역
        //    - isPrimary: 호출자가 지정한 주/부 동네 여부 적용
        UserRegion created = UserRegion.builder()
                .user(user) // 사용자 연결
                .region(target) // 지역 연결
                .isPrimary(primary) // 호출자가 지정한 주/부 동네 여부 적용
                .build();

        // 3) 데이터베이스에 저장: 새로운 UserRegion을 저장하여 연결을 완료합니다.
        userRegionRepository.save(created);
    }

    /**
     * 사용자의 현재 주 활동 동네 ID를 조회합니다.
     *
     * - isPrimary=true인 UserRegion을 찾아 해당 지역 ID를 반환합니다.
     * - 등록된 동네가 없으면 null을 반환합니다.
     *
     * @param userId 동네를 조회할 사용자 ID
     * @return 주 활동 동네의 지역 ID, 없으면 null
     * @throws GeneralException 사용자를 찾을 수 없을 때 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션: 데이터 변경 없이 조회만 수행합니다.
    public Long getPrimaryRegionId(Long userId) {
        // 1) 사용자 존재 확인: 주어진 ID로 사용자를 조회합니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2) 주 활동 동네 찾기:
        //    - findAllByUser: 해당 사용자의 모든 UserRegion을 조회합니다.
        //    - stream: 스트림 API를 사용하여 컬렉션을 처리합니다.
        //    - filter(UserRegion::isPrimary): isPrimary가 true인 것만 필터링합니다.
        //    - map: UserRegion에서 Region의 ID만 추출합니다.
        //    - findFirst: 첫 번째 결과를 Optional로 반환합니다.
        //    - orElse(null): 결과가 없으면 null을 반환합니다.
        return userRegionRepository.findAllByUser(user).stream()
                .filter(UserRegion::isPrimary) // isPrimary=true인 것만 선택
                .map(ur -> ur.getRegion().getRegionId()) // Region ID 추출
                .findFirst() // 첫 번째 결과 선택
                .orElse(null); // 없으면 null 반환
    }
}

