-- 채팅 목록 정렬/필터 성능
CREATE INDEX IF NOT EXISTS ix_chat_last_message_at ON chat (last_message_at DESC);
CREATE INDEX IF NOT EXISTS ix_chat_seller_id       ON chat (seller_id);
CREATE INDEX IF NOT EXISTS ix_chat_buyer_id        ON chat (buyer_id);

-- 메시지 테이블 인덱스 (테이블/컬럼명은 실제 스키마에 맞게 조정)
-- message(chat_id, sent_at, is_read) 라고 가정
CREATE INDEX IF NOT EXISTS ix_message_chat_sent_at ON message (chat_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS ix_message_is_read      ON message (is_read);
