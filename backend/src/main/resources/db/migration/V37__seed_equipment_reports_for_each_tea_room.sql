-- Seed three equipment reports for each tea room.

INSERT INTO equipment_report (tea_room_id, reported_by, title, content, status)
SELECT tr.id,
       tr.staff_user_id,
       CONCAT(tr.name, '照明系统巡检报障'),
       CONCAT('巡检发现', tr.name, '的主照明存在局部闪烁现象，晚间接待时亮度不稳定，建议尽快检查灯具、电源接口与控制开关。'),
       'PENDING'
FROM tea_room tr
WHERE tr.staff_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM equipment_report er
      WHERE er.tea_room_id = tr.id
        AND er.title = CONCAT(tr.name, '照明系统巡检报障')
  );

INSERT INTO equipment_report (tea_room_id, reported_by, title, content, status)
SELECT tr.id,
       tr.staff_user_id,
       CONCAT(tr.name, '空调出风异常报障'),
       CONCAT(tr.name, '在营业时段出现制冷响应慢、出风温度不均的问题，包间体感温度波动较大，需要排查滤网、风机和温控模块。'),
       'PROCESSING'
FROM tea_room tr
WHERE tr.staff_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM equipment_report er
      WHERE er.tea_room_id = tr.id
        AND er.title = CONCAT(tr.name, '空调出风异常报障')
  );

INSERT INTO equipment_report (tea_room_id, reported_by, title, content, status)
SELECT tr.id,
       tr.staff_user_id,
       CONCAT(tr.name, '茶台给排水检查记录'),
       CONCAT('例行检查中发现', tr.name, '茶台排水速度偏慢，注水测试后有轻微回流，已记录现场情况，建议安排维护人员检查排水管路与接口密封。'),
       'RESOLVED'
FROM tea_room tr
WHERE tr.staff_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM equipment_report er
      WHERE er.tea_room_id = tr.id
        AND er.title = CONCAT(tr.name, '茶台给排水检查记录')
  );
