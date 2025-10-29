DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes
    WHERE schemaname = 'public'
      AND indexname  = 'ux_chat_item_seller_buyer'
  ) THEN
    EXECUTE 'CREATE UNIQUE INDEX ux_chat_item_seller_buyer
             ON chat (item_id, seller_id, buyer_id)';
END IF;
END $$;
--ChatService에서 사용하는 코드와 더불어 안전장치