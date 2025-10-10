package com.salemale.domain.region.service; // 지역 데이터 임포트 서비스 인터페이스

/**
 * RegionImportService: CSV 파일에서 지역 데이터를 읽어와 데이터베이스에 적재하는 서비스 인터페이스입니다.
 *
 * - 애플리케이션 시작 시 classpath의 CSV 파일을 읽어 지역 데이터를 초기화합니다.
 * - UPSERT(INSERT or UPDATE) 방식으로 중복을 안전하게 처리합니다.
 * - 구현체(RegionImportServiceImpl)에서 실제 파일 파싱 및 데이터 저장 로직을 처리합니다.
 *
 * 주요 기능:
 * 1. CSV 파일 파싱: resources/data/region_data.csv를 읽어 파싱합니다.
 * 2. UPSERT 처리: 이미 존재하는 지역은 업데이트하고, 없으면 새로 삽입합니다.
 * 3. 초기 데이터 적재: 테이블이 비어있을 때만 임포트를 수행합니다.
 *
 * 사용 시나리오:
 * - 애플리케이션 최초 실행 시 지역 데이터를 자동으로 적재합니다.
 * - RegionImportRunner가 이 서비스를 호출하여 초기화를 수행합니다.
 */
public interface RegionImportService {

    /**
     * Classpath의 CSV 파일에서 지역 데이터를 읽어와 데이터베이스에 적재합니다.
     *
     * - resources/data/region_data.csv 파일을 읽습니다.
     * - 각 레코드를 파싱하여 Region으로 변환합니다.
     * - UPSERT 방식으로 데이터베이스에 저장합니다(중복 시 업데이트).
     * - 테이블에 이미 데이터가 많이 있으면 임포트를 건너뜁니다(100개 초과 시).
     *
     * CSV 파일 형식:
     * - 헤더: sido, sigungu, eupmyeondong, latitude, longitude
     * - 데이터: "서울특별시","강남구","역삼동",37.4979,127.0376
     * - 따옴표로 감싼 값 내부의 콤마를 지원합니다.
     *
     * UPSERT 동작:
     * - 유니크 제약(sido, sigungu, eupmyeondong)을 기준으로 중복을 확인합니다.
     * - 이미 존재하면 위경도를 업데이트하고, 없으면 새로 삽입합니다.
     * - ON CONFLICT (PostgreSQL) 또는 유사한 메커니즘을 사용합니다.
     *
     * @return 처리된 레코드 수(성공적으로 UPSERT된 행의 개수)
     * @throws IllegalStateException CSV 파일을 찾을 수 없거나 파싱에 실패한 경우 발생
     */
    int importFromClasspathCsv();
}
