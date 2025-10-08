package com.salemale.domain.region.service; // 앱 시작 시 조건부로 지역 데이터를 적재하는 러너 컴포넌트

import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.beans.factory.annotation.Value; // 설정값 주입
import org.springframework.boot.ApplicationArguments; // 애플리케이션 실행 인자
import org.springframework.boot.ApplicationRunner; // 부트 실행 완료 후 자동 실행되는 훅
import org.springframework.stereotype.Component; // 스프링 빈 선언

/**
 * RegionImportRunner: 스프링 부트 애플리케이션 시작 시 지역 데이터를 자동으로 적재하는 컴포넌트입니다.
 *
 * - ApplicationRunner 인터페이스를 구현하여 애플리케이션 초기화 완료 후 자동 실행됩니다.
 * - 설정 플래그(app.region.import.enabled)로 임포트 실행 여부를 제어합니다.
 * - RegionImportService를 호출하여 실제 CSV 파일 임포트를 수행합니다.
 *
 * 동작 방식:
 * 1. 스프링 부트가 완전히 시작된 후 run() 메서드가 자동 호출됩니다.
 * 2. app.region.import.enabled 설정값을 확인합니다.
 * 3. true이면 RegionImportService를 호출하여 CSV 임포트를 실행합니다.
 * 4. false이면 임포트를 건너뜁니다(기본값: false).
 *
 * 설정 방법:
 * - application.yml에 다음과 같이 설정합니다:
 *   app:
 *     region:
 *       import:
 *         enabled: true  # 임포트 활성화
 *
 * 주의사항:
 * - 운영 환경에서는 false로 설정하여 매번 임포트되지 않도록 합니다.
 * - 개발 환경에서는 true로 설정하여 초기 데이터를 자동 적재할 수 있습니다.
 * - Flyway/Liquibase를 사용하는 경우 이 러너를 비활성화하고 마이그레이션으로 관리할 수도 있습니다.
 */
@Component // 스프링이 이 클래스를 빈으로 등록하고, ApplicationRunner로 자동 인식합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
@Slf4j // Lombok: log 객체를 자동으로 생성하여 임포트 상태를 기록할 수 있게 합니다.
public class RegionImportRunner implements ApplicationRunner { // ApplicationRunner 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final RegionImportService regionImportService; // CSV 파일에서 지역 데이터를 읽어 저장하는 서비스

    // 설정값 주입: application.yml의 app.region.import.enabled 값을 읽어옵니다.
    // - @Value: Spring의 프로퍼티 값을 필드에 주입하는 애노테이션입니다.
    // - ${app.region.import.enabled:false}: 설정값이 없으면 기본값 false를 사용합니다.
    // - 운영 환경에서는 false(기본값)로 두어 매번 임포트되지 않도록 합니다.
    @Value("${app.region.import.enabled:false}")
    private boolean importEnabled; // 임포트 실행 여부를 제어하는 플래그

    /**
     * 애플리케이션 시작 시 자동 실행되는 메서드입니다.
     *
     * - 스프링 부트가 완전히 초기화된 후 한 번만 실행됩니다.
     * - importEnabled 플래그를 확인하여 임포트 여부를 결정합니다.
     * - true이면 RegionImportService를 호출하여 CSV 데이터를 적재합니다.
     *
     * @param args 애플리케이션 실행 인자(사용되지 않음)
     */
    @Override // ApplicationRunner 인터페이스 메서드 구현을 명시적으로 표시
    public void run(ApplicationArguments args) {
        // 1) 임포트 플래그 확인: importEnabled가 false이면 임포트를 건너뜁니다.
        if (!importEnabled) {
            // 1-1) 로그 기록: 임포트가 비활성화되었음을 알립니다.
            log.info("Region import skipped (app.region.import.enabled=false)");
            return; // 메서드를 종료하여 임포트를 실행하지 않습니다.
        }

        // 2) 임포트 실행: RegionImportService를 호출하여 CSV 파일을 읽고 데이터베이스에 적재합니다.
        //    - importFromClasspathCsv(): resources/data/region_data.csv를 읽어 UPSERT를 수행합니다.
        //    - 반환값: 처리된(UPSERT된) 레코드 수
        int count = regionImportService.importFromClasspathCsv();

        // 3) 로그 기록: 임포트가 완료되었음을 알리고 처리된 레코드 수를 표시합니다.
        //    - 예: "Region import completed: 1234 upserted"
        log.info("Region import completed: {} upserted", count);
    }
}
