package com.salemale.domain.region.service; // CSV → DB 안전 병합(UPSERT) 서비스 구현체

import com.salemale.domain.region.repository.RegionRepository; // 데이터 저장/병합 레이어
import lombok.RequiredArgsConstructor; // Lombok: 생성자 자동 생성
import lombok.extern.slf4j.Slf4j; // Lombok: 로깅 지원
import org.springframework.stereotype.Service; // 스프링 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 경계

import java.io.BufferedReader; // 파일 읽기
import java.io.InputStream; // 리소스 스트림
import java.io.InputStreamReader; // 바이트 스트림 → 문자 스트림
import java.math.BigDecimal; // 위경도 정밀도
import java.nio.charset.StandardCharsets; // UTF-8 BOM 안전
import java.util.Objects; // null 체크 유틸리티
import jakarta.persistence.EntityManager; // 네이티브 쿼리 실행용

/**
 * RegionImportServiceImpl: CSV 파일에서 지역 데이터를 읽어 데이터베이스에 적재하는 서비스 구현체입니다.
 *
 * - RegionImportService 인터페이스를 구현합니다.
 * - Classpath의 region_data.csv 파일을 스트리밍 방식으로 파싱합니다.
 * - UPSERT(INSERT or UPDATE) 방식으로 중복을 안전하게 처리합니다.
 *
 * 동작 원리:
 * 1. 테이블에 이미 데이터가 있는지 확인합니다(100개 초과 시 건너뜀).
 * 2. 유니크 인덱스가 존재하는지 확인하고, 없으면 생성합니다.
 * 3. CSV 파일을 한 줄씩 읽어 파싱합니다.
 * 4. 각 레코드를 UPSERT 쿼리로 데이터베이스에 적재합니다.
 *
 * UPSERT 전략:
 * - PostgreSQL의 ON CONFLICT ... DO UPDATE 구문을 사용합니다.
 * - 유니크 제약(sido, sigungu, eupmyeondong)을 기준으로 중복을 확인합니다.
 * - 이미 존재하면 위경도를 업데이트하고, 없으면 새로 삽입합니다.
 *
 * CSV 파싱:
 * - 따옴표로 감싼 값을 지원합니다(예: "서울특별시").
 * - 따옴표 내부의 콤마를 올바르게 처리합니다.
 * - 이스케이프된 따옴표("")도 처리합니다.
 */
@Service // 스프링이 이 클래스를 서비스 빈으로 등록하여 다른 곳에서 주입받을 수 있게 합니다.
@RequiredArgsConstructor // Lombok: final 필드를 매개변수로 받는 생성자를 자동 생성합니다.
@Slf4j // Lombok: log 객체를 자동으로 생성하여 임포트 진행 상황을 기록할 수 있게 합니다.
public class RegionImportServiceImpl implements RegionImportService { // RegionImportService 인터페이스 구현

    // 의존성 선언: RequiredArgsConstructor로 자동 주입됩니다.
    private final RegionRepository regionRepository; // 지역 데이터를 저장하고 UPSERT를 수행하는 저장소
    private final EntityManager entityManager; // 네이티브 SQL 쿼리를 실행하기 위한 JPA EntityManager

    /**
     * Classpath의 CSV 파일에서 지역 데이터를 읽어와 데이터베이스에 적재합니다.
     *
     * - resources/data/region_data.csv 파일을 읽습니다.
     * - 각 레코드를 파싱하여 Region으로 변환합니다.
     * - UPSERT 방식으로 데이터베이스에 저장합니다(중복 시 업데이트).
     * - 테이블에 이미 데이터가 많이 있으면 임포트를 건너뜁니다(100개 초과 시).
     *
     * @return 처리된 레코드 수(성공적으로 UPSERT된 행의 개수)
     * @throws IllegalStateException CSV 파일을 찾을 수 없거나 파싱에 실패한 경우 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 모든 UPSERT 작업을 하나의 트랜잭션으로 묶어 일관성을 보장합니다.
    public int importFromClasspathCsv() {
        int processed = 0; // 처리된 레코드 수를 추적합니다.

        try {
            // 1) 중복 임포트 방지: 테이블에 이미 데이터가 충분히 있으면 임포트를 건너뜁니다.
            //    - count(): 테이블의 전체 레코드 수를 조회합니다.
            //    - 100개 초과: 이미 초기 데이터가 적재되었다고 간주합니다.
            long total = regionRepository.count();
            if (total > 100) {
                log.info("Region table already has {} rows. Skip import.", total);
                return 0; // 임포트를 건너뛰고 0을 반환합니다.
            }

            // 2) 유니크 인덱스 보장: UPSERT가 동작하려면 유니크 제약이 필요합니다.
            //    - ensureUniqueIndex(): 인덱스가 없으면 생성합니다(멱등성 보장).
            ensureUniqueIndex();

            // 3) CSV 파일 열기: Classpath에서 region_data.csv 파일을 읽습니다.
            //    - getResourceAsStream: 클래스패스의 리소스를 InputStream으로 엽니다.
            //    - Objects.requireNonNull: 파일이 없으면 예외를 던집니다.
            InputStream is = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream("data/region_data.csv"),
                    "resources/data/region_data.csv not found"
            );

            // 4) CSV 파일 파싱: UTF-8 인코딩으로 한 줄씩 읽습니다.
            //    - try-with-resources: 파일을 자동으로 닫습니다.
            //    - BufferedReader: 효율적인 줄 단위 읽기를 지원합니다.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                // 5) 헤더 스킵: 첫 줄은 컬럼 이름이므로 건너뜁니다.
                String header = reader.readLine();
                if (header == null) return 0; // 파일이 비어있으면 종료

                // 6) 데이터 라인 처리: 각 줄을 파싱하여 UPSERT합니다.
                String line;
                while ((line = reader.readLine()) != null) { // 파일 끝까지 반복
                    // 6-1) CSV 라인 파싱: 콤마로 분리하되 따옴표 내부는 보호합니다.
                    String[] parts = splitCsvLine(line);

                    // 6-2) 필드 수 검증: 최소 5개(sido, sigungu, eupmyeondong, latitude, longitude) 필요
                    if (parts.length < 5) {
                        log.warn("skip invalid line: {}", line);
                        continue; // 잘못된 라인은 건너뜁니다.
                    }

                    // 6-3) 필드 추출 및 정제: 따옴표 제거 및 공백 제거
                    String sido = unquote(parts[0].trim()); // 시도(예: 서울특별시)
                    String sigungu = unquote(parts[1].trim()); // 시군구(예: 강남구)
                    String eupmyeondong = unquote(parts[2].trim()); // 읍면동(예: 역삼동)
                    BigDecimal latitude = new BigDecimal(unquote(parts[3].trim())); // 위도
                    BigDecimal longitude = new BigDecimal(unquote(parts[4].trim())); // 경도

                    // 6-4) UPSERT 실행: 네이티브 쿼리로 데이터베이스에 병합합니다.
                    //      - 이미 존재하면 위경도를 업데이트합니다.
                    //      - 없으면 새로 삽입합니다.
                    regionRepository.upsert(sido, sigungu, eupmyeondong, latitude, longitude);
                    processed++; // 처리 카운트 증가
                }
            }
        } catch (Exception e) {
            // 7) 예외 처리: 파일을 찾을 수 없거나 파싱에 실패하면 로그를 남기고 예외를 던집니다.
            log.error("Region CSV import failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Region CSV import failed", e);
        }

        // 8) 결과 반환: 처리된 레코드 수를 반환합니다.
        return processed;
    }

    /**
     * 내부 메서드: UPSERT 동작에 필요한 유니크 인덱스를 생성합니다.
     *
     * - ON CONFLICT (sido, sigungu, eupmyeondong)가 동작하려면 해당 컬럼 조합에 유니크 제약이 필요합니다.
     * - CREATE UNIQUE INDEX IF NOT EXISTS를 사용하여 멱등성을 보장합니다.
     * - 인덱스가 이미 존재하면 아무 일도 일어나지 않습니다.
     *
     * 주의사항:
     * - 이 메서드는 네이티브 쿼리를 실행하므로 데이터베이스 종속적입니다(PostgreSQL 기준).
     * - 인덱스 생성 권한이 없거나 경쟁 조건이 발생하면 경고를 남기고 계속 진행합니다.
     */
    private void ensureUniqueIndex() {
        try {
            // 1) 네이티브 쿼리 실행: 유니크 인덱스가 없으면 생성합니다.
            //    - IF NOT EXISTS: 이미 존재하면 건너뜁니다(멱등성).
            //    - (sido, sigungu, eupmyeondong): 세 컬럼 조합으로 유니크 제약을 만듭니다.
            entityManager.createNativeQuery(
                    "CREATE UNIQUE INDEX IF NOT EXISTS ux_region_sido_sigungu_eupmyeondong ON region (sido, sigungu, eupmyeondong)"
            ).executeUpdate();
        } catch (Exception e) {
            // 2) 예외 처리: 인덱스 생성 실패 시 경고를 남기지만 임포트는 계속 진행합니다.
            //    - 실패 원인: 권한 부족, 동시 생성 경쟁, DB 종류 불일치 등
            //    - 이미 Flyway/Liquibase로 인덱스를 생성했다면 이 단계는 건너뛸 수 있습니다.
            log.warn("ensureUniqueIndex failed: {}", e.getMessage());
        }
    }

    /**
     * 내부 메서드: CSV 라인을 파싱하여 필드 배열로 변환합니다.
     *
     * - 따옴표로 감싼 값 내부의 콤마를 올바르게 처리합니다.
     * - 이스케이프된 따옴표("")도 처리합니다.
     * - 간단한 상태 머신 방식으로 구현되어 외부 라이브러리 없이 동작합니다.
     *
     * 예시:
     * - 입력: "서울특별시","강남구","역삼동",37.4979,127.0376
     * - 출력: ["서울특별시", "강남구", "역삼동", "37.4979", "127.0376"]
     *
     * @param line 파싱할 CSV 라인
     * @return 파싱된 필드 배열
     */
    private static String[] splitCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>(); // 결과를 저장할 리스트
        StringBuilder current = new StringBuilder(); // 현재 필드를 누적
        boolean inQuotes = false; // 따옴표 안인지 밖인지 추적

        // 1) 문자열을 순회하며 상태를 추적합니다.
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // 2) 따옴표 처리: 시작/종료 또는 이스케이프된 따옴표
            if (c == '"') {
                // 2-1) 이스케이프된 따옴표("") 처리: 연속된 두 따옴표는 하나의 따옴표로 간주
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); // 실제 따옴표 문자로 추가
                    i++; // 다음 따옴표 스킵
                } else {
                    // 2-2) 일반 따옴표: 따옴표 안/밖 상태를 토글
                    inQuotes = !inQuotes;
                }
            }
            // 3) 콤마 처리: 따옴표 밖의 콤마는 필드 구분자
            else if (c == ',' && !inQuotes) {
                fields.add(current.toString()); // 현재 필드를 리스트에 추가
                current.setLength(0); // 버퍼 초기화
            }
            // 4) 일반 문자: 현재 필드에 추가
            else {
                current.append(c);
            }
        }

        // 5) 마지막 필드 추가: 루프가 끝나면 남은 내용을 추가
        fields.add(current.toString());

        // 6) 배열로 변환하여 반환
        return fields.toArray(new String[0]);
    }

    /**
     * 내부 메서드: 문자열 앞뒤의 따옴표를 제거합니다.
     *
     * - CSV 파싱 후 따옴표로 감싸진 값에서 따옴표를 제거합니다.
     * - 예: "서울특별시" → 서울특별시
     *
     * @param s 따옴표 제거 대상 문자열
     * @return 따옴표가 제거된 문자열
     */
    private static String unquote(String s) {
        // 1) 길이 확인: 최소 2글자 이상이어야 따옴표 쌍이 있을 수 있습니다.
        // 2) 시작/끝 확인: 첫 문자와 마지막 문자가 모두 따옴표인지 확인합니다.
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            // 3) 따옴표 제거: 첫 문자와 마지막 문자를 제외한 부분을 반환합니다.
            return s.substring(1, s.length() - 1);
        }

        // 4) 따옴표가 없으면 원본 그대로 반환
        return s;
    }
}

