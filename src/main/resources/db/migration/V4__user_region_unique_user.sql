-- user_region: 각 사용자당 하나의 지역만 갖도록 유니크 제약 추가
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_region_user'
  ) THEN
    ALTER TABLE user_region
      ADD CONSTRAINT uk_user_region_user UNIQUE (user_id);
  END IF;
END$$;


