-- 안전 보강: 유니크 제약이 누락된 환경에서 ON CONFLICT 실패를 방지
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_region_admin_triple'
  ) THEN
    ALTER TABLE region
      ADD CONSTRAINT uk_region_admin_triple UNIQUE (sido, sigungu, eupmyeondong);
  END IF;
END$$;


