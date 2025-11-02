-- chat.buyer_id 가 NULL 이면 item.winner_id 로 채움
UPDATE chat c
SET buyer_id = i.winner_id
    FROM item i
WHERE c.item_id = i.item_id
  AND c.buyer_id IS NULL
  AND i.winner_id IS NOT NULL;
--이미 채워져 있으면 영향 없음
