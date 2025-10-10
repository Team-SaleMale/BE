-- 지역 테이블 생성 (시/군구/읍면동 + 위경도)
-- 원리
--  - 행정 3단계(sido,sigungu,eupmyeondong)를 유니크키로 정의하여 중복을 방지합니다.
--  - 좌표는 수치 정렬/연산을 위해 NUMERIC(18,10)로 저장합니다.
--  - BaseEntity(created_at, updated_at, deleted_at)와 호환되도록 기본값/트리거를 둡니다.

CREATE TABLE IF NOT EXISTS region (
    region_id      BIGSERIAL PRIMARY KEY,
    sido           VARCHAR(50)  NOT NULL,      -- 시/도
    sigungu        VARCHAR(50)  NOT NULL,      -- 시/군/구
    eupmyeondong   VARCHAR(50)  NOT NULL,      -- 읍/면/동
    latitude       NUMERIC(18,10) NOT NULL,    -- 위도(WGS84)
    longitude      NUMERIC(18,10) NOT NULL,    -- 경도(WGS84)
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMP NULL
);

-- 복합 조회에 유리한 인덱스 (행정 3단계)
CREATE INDEX IF NOT EXISTS idx_region_sido_sigungu_eupmyeondong
  ON region (sido, sigungu, eupmyeondong);

-- 시군구 단위 조회 최적화
CREATE INDEX IF NOT EXISTS idx_region_sigungu
  ON region (sigungu);

-- 동일 행정구역의 중복 입력 방지 (업무 규칙상 유일)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_region_admin_triple'
  ) THEN
    ALTER TABLE region
      ADD CONSTRAINT uk_region_admin_triple UNIQUE (sido, sigungu, eupmyeondong);
  END IF;
END$$;

-- 트리거로 updated_at 자동 갱신 (BaseEntity와 정합)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_region_set_updated_at'
  ) THEN
    CREATE TRIGGER trg_region_set_updated_at
    BEFORE UPDATE ON region
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;
