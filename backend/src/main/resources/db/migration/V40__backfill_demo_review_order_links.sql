SET @USER1_ID := (SELECT id FROM sys_user WHERE username = 'user1' LIMIT 1);
SET @ROOM1_ID := (SELECT id FROM tea_room WHERE name = '茶室 云栖 01' LIMIT 1);
SET @ROOM2_ID := (SELECT id FROM tea_room WHERE name = '茶室 松风 02' LIMIT 1);

SET @ROOM1_COMPLETED_RESERVATION_ID := (
    SELECT id
    FROM reservation
    WHERE user_id = @USER1_ID
      AND tea_room_id = @ROOM1_ID
      AND status = 'COMPLETED'
    ORDER BY reserved_end_time DESC, id DESC
    LIMIT 1
);

UPDATE product_order
SET reservation_id = @ROOM1_COMPLETED_RESERVATION_ID
WHERE order_no = 'ORD-DEMO-0003'
  AND reservation_id IS NULL
  AND @ROOM1_COMPLETED_RESERVATION_ID IS NOT NULL;

UPDATE review
SET reservation_id = @ROOM1_COMPLETED_RESERVATION_ID
WHERE user_id = @USER1_ID
  AND tea_room_id = @ROOM1_ID
  AND content = '环境安静，茶香很好，服务到位。'
  AND reservation_id IS NULL
  AND @ROOM1_COMPLETED_RESERVATION_ID IS NOT NULL;

INSERT INTO reservation (user_id, tea_room_id, reserved_start_time, reserved_end_time, status, remark)
SELECT @USER1_ID,
       @ROOM2_ID,
       DATE_ADD(NOW(), INTERVAL -6 DAY),
       DATE_ADD(DATE_ADD(NOW(), INTERVAL -6 DAY), INTERVAL 2 HOUR),
       'COMPLETED',
       '历史演示数据回填'
WHERE @USER1_ID IS NOT NULL
  AND @ROOM2_ID IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM reservation
      WHERE user_id = @USER1_ID
        AND tea_room_id = @ROOM2_ID
        AND status = 'COMPLETED'
  );

SET @ROOM2_COMPLETED_RESERVATION_ID := (
    SELECT id
    FROM reservation
    WHERE user_id = @USER1_ID
      AND tea_room_id = @ROOM2_ID
      AND status = 'COMPLETED'
    ORDER BY reserved_end_time DESC, id DESC
    LIMIT 1
);

INSERT INTO product_order (order_no, user_id, reservation_id, amount, status, payment_method)
SELECT 'ORD-DEMO-0004', @USER1_ID, @ROOM2_COMPLETED_RESERVATION_ID, 156.00, 'COMPLETED', 'WECHAT'
WHERE @USER1_ID IS NOT NULL
  AND @ROOM2_COMPLETED_RESERVATION_ID IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_order WHERE order_no = 'ORD-DEMO-0004'
  );

UPDATE review
SET reservation_id = @ROOM2_COMPLETED_RESERVATION_ID
WHERE user_id = @USER1_ID
  AND tea_room_id = @ROOM2_ID
  AND content = '设备齐全，整体不错，建议增加茶点选择。'
  AND reservation_id IS NULL
  AND @ROOM2_COMPLETED_RESERVATION_ID IS NOT NULL;
