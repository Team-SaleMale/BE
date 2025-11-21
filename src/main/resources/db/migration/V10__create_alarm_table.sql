-- 알림(Alarm) 테이블 생성
--  - 사용자(user_id)별 알림(content, 생성/수정/삭제 시각)을 저장
--  - 읽음 상태 컬럼(is_read, read_at) 포함
--  - 사용자별 최신/미읽음 조회를 위해 인덱스 구성

CREATE TABLE IF NOT EXISTS alarm (
    alarm_id   BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    content    TEXT          NOT NULL,
    is_read    BOOLEAN       NOT NULL DEFAULT FALSE,  -- 읽음 여부
    read_at    TIMESTAMP(6),                          -- 읽은 시각
    created_at TIMESTAMP(6)  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6),
    deleted_at TIMESTAMP(6)
    );

ALTER TABLE alarm
    ADD CONSTRAINT fk_alarm_user
        FOREIGN KEY (user_id) REFERENCES users(user_id);

-- 최신 알림 조회
CREATE INDEX IF NOT EXISTS idx_alarm_user_created_at
    ON alarm (user_id, created_at DESC);

-- 미읽음 필터
CREATE INDEX IF NOT EXISTS idx_alarm_user_unread_created
    ON alarm (user_id, is_read, created_at DESC);
