-- 아래 FK 이름(fk_chat_*)은 없으면 새로 추가합니다.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_name = 'chat'
      AND constraint_name = 'fk_chat_seller'
  ) THEN
    EXECUTE 'ALTER TABLE chat
             ADD CONSTRAINT fk_chat_seller
             FOREIGN KEY (seller_id) REFERENCES users(id)';
END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_name = 'chat'
      AND constraint_name = 'fk_chat_buyer'
  ) THEN
    EXECUTE 'ALTER TABLE chat
             ADD CONSTRAINT fk_chat_buyer
             FOREIGN KEY (buyer_id) REFERENCES users(id)';
END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_name = 'chat'
      AND constraint_name = 'fk_chat_item'
  ) THEN
    EXECUTE 'ALTER TABLE chat
             ADD CONSTRAINT fk_chat_item
             FOREIGN KEY (item_id) REFERENCES item(item_id)';
END IF;
END $$;
