-- 알림(Alarm) 테이블 확장
--  - 기존 alarm 테이블에 읽음 상태 컬럼(is_read, read_at)을 추가

-- 1) 읽음 상태 컬럼 추가
ALTER TABLE alarm
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP(6);

-- 2) FK (users 테이블 PK가 id라고 가정)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_alarm_user'
    ) THEN
ALTER TABLE alarm
    ADD CONSTRAINT fk_alarm_user
        FOREIGN KEY (user_id) REFERENCES users(id);
END IF;
END$$;

-- 3) 최신 알림 조회 인덱스
CREATE INDEX IF NOT EXISTS idx_alarm_user_created_at
    ON alarm (user_id, created_at DESC);

-- 4) 미읽음 필터 + 최신순 인덱스
CREATE INDEX IF NOT EXISTS idx_alarm_user_unread_created
    ON alarm (user_id, is_read, created_at DESC);
