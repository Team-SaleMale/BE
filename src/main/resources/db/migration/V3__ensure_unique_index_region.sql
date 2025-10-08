-- 보강: 유니크 인덱스가 없어 ON CONFLICT가 실패하는 환경 대응
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_indexes
    WHERE schemaname = current_schema()
      AND indexname = 'ux_region_sido_sigungu_eupmyeondong'
  ) THEN
    CREATE UNIQUE INDEX ux_region_sido_sigungu_eupmyeondong
      ON region (sido, sigungu, eupmyeondong);
  END IF;
END$$;


