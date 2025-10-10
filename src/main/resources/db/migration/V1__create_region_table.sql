-- 吏???뚯씠釉??앹꽦
CREATE TABLE IF NOT EXISTS region (
    region_id      BIGSERIAL PRIMARY KEY,
    sido           VARCHAR(50)  NOT NULL,
    sigungu        VARCHAR(50)  NOT NULL,
    eupmyeondong   VARCHAR(50)  NOT NULL,
    latitude       NUMERIC(18,10) NOT NULL,
    longitude      NUMERIC(18,10) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_region_sido_sigungu_eupmyeondong
  ON region (sido, sigungu, eupmyeondong);

CREATE INDEX IF NOT EXISTS idx_region_sigungu
  ON region (sigungu);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_region_admin_triple'
  ) THEN
    ALTER TABLE region
      ADD CONSTRAINT uk_region_admin_triple UNIQUE (sido, sigungu, eupmyeondong);
  END IF;
END$$;

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
