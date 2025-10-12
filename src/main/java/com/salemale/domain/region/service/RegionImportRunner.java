package com.salemale.domain.region.service; // 앱 시작 시 조건부로 지역 데이터를 적재하는 러너 컴포넌트

import com.salemale.domain.region.repository.RegionRepository; // 지역 데이터 개수 확인용
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.boot.ApplicationArguments; // 애플리케이션 실행 인자
import org.springframework.boot.ApplicationRunner; // 부트 실행 완료 후 자동 실행되는 훅
import org.springframework.stereotype.Component; // 스프링 빈 선언

/**
 * RegionImportRunner: 스프링 부트 애플리케이션 시작 시 지역 데이터를 자동으로 적재하는 컴포넌트입니다.
 *
 * - ApplicationRunner 인터페이스를 구현하여 애플리케이션 초기화 완료 후 자동 실행됩니다.
 * - **스마트 임포트**: Region 테이블이 비어있을 때만 CSV 데이터를 적재합니다.
 * - RegionImportService를 호출하여 실제 CSV 파일 임포트를 수행합니다.
 *
 * 동작 방식:
 * 1. 스프링 부트가 완전히 시작된 후 run() 메서드가 자동 호출됩니다.
 * 2. Region 테이블의 레코드 개수를 확인합니다.
 * 3. 테이블이 비어있으면 CSV 파일에서 데이터를 임포트합니다.
 * 4. 이미 데이터가 있으면 임포트를 건너뜁니다(빠른 시작).
 *
 * 장점:
 * - 개발 환경: 최초 실행 시 자동으로 초기 데이터 로드
 * - 운영 환경: 이미 데이터가 있으면 CSV 파싱과 UPSERT 비용 절약
 * - 환경변수 불필요: 별도 설정 없이 자동으로 판단
 *
 * 주의사항:
 * - 데이터가 손상되어 재임포트가 필요한 경우 Region 테이블을 비우고 재시작하세요.
 * - Flyway/Liquibase를 사용하는 경우 이 러너와 중복되지 않도록 주의하세요.
 */
@Component // 스프링이 이 클래스를 빈으로 등록하고, ApplicationRunner로 자동 인식합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
@Slf4j // Lombok: log 객체를 자동으로 생성하여 임포트 상태를 기록할 수 있게 합니다.
public class RegionImportRunner implements ApplicationRunner { // ApplicationRunner 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final RegionImportService regionImportService; // CSV 파일에서 지역 데이터를 읽어 저장하는 서비스
    private final RegionRepository regionRepository; // 지역 데이터 존재 여부 확인용

    /**
     * 애플리케이션 시작 시 자동 실행되는 메서드입니다.
     *
     * - 스프링 부트가 완전히 초기화된 후 한 번만 실행됩니다.
     * - Region 테이블이 비어있는지 확인합니다.
     * - 비어있으면 CSV 파일에서 데이터를 임포트합니다.
     * - 이미 데이터가 있으면 임포트를 건너뜁니다.
     *
     * @param args 애플리케이션 실행 인자(사용되지 않음)
     */
    @Override // ApplicationRunner 인터페이스 메서드 구현을 명시적으로 표시
    public void run(ApplicationArguments args) {
        try {
            // 1) 기존 데이터 확인: Region 테이블에 이미 데이터가 있는지 체크합니다.
            //    - count(): JPA가 제공하는 메서드로 전체 레코드 수를 반환합니다.
            long existingCount = regionRepository.count();
            
            if (existingCount > 0) {
                // 1-1) 데이터가 이미 있으면 임포트를 건너뜁니다.
                //      - 운영 환경: 빠른 시작 (CSV 파싱과 UPSERT 비용 절약)
                //      - 개발 환경: 재시작 시 불필요한 재임포트 방지
                log.info("Region import skipped: {} regions already exist in database", existingCount);
                return; // 메서드를 종료하여 임포트를 실행하지 않습니다.
            }

            // 2) 임포트 실행: 테이블이 비어있으면 CSV 파일에서 데이터를 적재합니다.
            //    - importFromClasspathCsv(): resources/data/region_data.csv를 읽어 UPSERT를 수행합니다.
            //    - 반환값: 처리된(UPSERT된) 레코드 수
            log.info("Region table is empty. Starting CSV import...");
            int count = regionImportService.importFromClasspathCsv();
            
            // 3) 로그 기록: 임포트가 완료되었음을 알리고 처리된 레코드 수를 표시합니다.
            //    - 예: "Region import completed: 1234 regions imported"
            log.info("Region import completed: {} regions imported successfully", count);
            
        } catch (Exception e) {
            // 4) 임포트 실패 시: 에러 로그를 남기고 애플리케이션은 정상 시작합니다.
            //    - 파일 누락, 파싱 오류, DB 연결 실패 등 다양한 예외를 처리합니다.
            //    - 운영 환경에서는 임포트 없이도 정상 동작할 수 있도록 합니다.
            log.error("Region import failed: {}. Application will continue without import data.", 
                    e.getMessage(), e);
        }
    }
}
