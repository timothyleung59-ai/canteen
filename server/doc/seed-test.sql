-- ============================================================
--  本机验证用测试数据 (仅 localtest)，幂等：可重复执行
--  appid = wxtest0000000000，与 localtest compose 一致
--  3 部门 / 6 员工 / 一批报餐记录(今/昨/明预约) / 1 行配置
-- ============================================================
SET NAMES utf8mb4;
SET @APPID := 'wxtest0000000000';

-- 清掉本 appid 旧数据（按外键安全顺序）
DELETE FROM bc_record           WHERE app_id = @APPID;
DELETE FROM bc_reserve_record   WHERE app_id = @APPID;
DELETE FROM bc_user             WHERE app_id = @APPID;
DELETE FROM bc_user_department  WHERE app_id = @APPID;
DELETE FROM bc_config           WHERE app_id = @APPID;

-- 配置（午/晚餐均开放，免审核）
INSERT INTO bc_config
  (user_need_approve, saturday_can_diner, sunday_can_diner, end_time, app_id,
   lunch_order_time, dinner_order_time, lunch_can_meal, dinner_can_meal)
VALUES
  (b'0', b'0', b'0', '09:30', @APPID, '09:30', '15:30', b'1', b'1');

-- 部门
INSERT INTO bc_user_department (name, app_id) VALUES ('行政部', @APPID); SET @D1 := LAST_INSERT_ID();
INSERT INTO bc_user_department (name, app_id) VALUES ('技术部', @APPID); SET @D2 := LAST_INSERT_ID();
INSERT INTO bc_user_department (name, app_id) VALUES ('后勤部', @APPID); SET @D3 := LAST_INSERT_ID();

-- 员工（status=1 已激活）
INSERT INTO bc_user (addtime, deletestatus, REVISION, app_id, name, user_department_id, mobile, status)
  VALUES (NOW(),'0',0,@APPID,'张伟', @D1,'13800000001',1); SET @U1 := LAST_INSERT_ID();
INSERT INTO bc_user (addtime, deletestatus, REVISION, app_id, name, user_department_id, mobile, status)
  VALUES (NOW(),'0',0,@APPID,'李娜', @D1,'13800000002',1); SET @U2 := LAST_INSERT_ID();
INSERT INTO bc_user (addtime, deletestatus, REVISION, app_id, name, user_department_id, mobile, status)
  VALUES (NOW(),'0',0,@APPID,'王强', @D2,'13800000003',1); SET @U3 := LAST_INSERT_ID();
INSERT INTO bc_user (addtime, deletestatus, REVISION, app_id, name, user_department_id, mobile, status)
  VALUES (NOW(),'0',0,@APPID,'刘洋', @D2,'13800000004',1); SET @U4 := LAST_INSERT_ID();
INSERT INTO bc_user (addtime, deletestatus, REVISION, app_id, name, user_department_id, mobile, status)
  VALUES (NOW(),'0',0,@APPID,'陈静', @D3,'13800000005',1); SET @U5 := LAST_INSERT_ID();
INSERT INTO bc_user (addtime, deletestatus, REVISION, app_id, name, user_department_id, mobile, status)
  VALUES (NOW(),'0',0,@APPID,'赵磊', @D3,'13800000006',1); SET @U6 := LAST_INSERT_ID();

-- 报餐记录
--   bc_type: 1=中餐 2=晚餐    bc_channel: 0=手动 1=预约    had_eat: 0/1
-- 今天(2026-06-16) 午餐 5 人
INSERT INTO bc_record (addtime, deletestatus, REVISION, app_id, user_id, bc_type, bc_channel, dintime, had_eat) VALUES
 (NOW(),'0',0,@APPID,@U1,1,0,'2026-06-16 11:30:00',0),
 (NOW(),'0',0,@APPID,@U2,1,0,'2026-06-16 11:31:00',0),
 (NOW(),'0',0,@APPID,@U3,1,0,'2026-06-16 11:32:00',1),
 (NOW(),'0',0,@APPID,@U4,1,0,'2026-06-16 11:33:00',0),
 (NOW(),'0',0,@APPID,@U5,1,0,'2026-06-16 11:34:00',1);
-- 今天(2026-06-16) 晚餐 3 人
INSERT INTO bc_record (addtime, deletestatus, REVISION, app_id, user_id, bc_type, bc_channel, dintime, had_eat) VALUES
 (NOW(),'0',0,@APPID,@U1,2,0,'2026-06-16 17:30:00',0),
 (NOW(),'0',0,@APPID,@U3,2,0,'2026-06-16 17:31:00',0),
 (NOW(),'0',0,@APPID,@U5,2,0,'2026-06-16 17:32:00',0);
-- 昨天(2026-06-15) 午餐 3 人（已就餐）
INSERT INTO bc_record (addtime, deletestatus, REVISION, app_id, user_id, bc_type, bc_channel, dintime, had_eat) VALUES
 (NOW(),'0',0,@APPID,@U1,1,0,'2026-06-15 11:30:00',1),
 (NOW(),'0',0,@APPID,@U2,1,0,'2026-06-15 11:31:00',1),
 (NOW(),'0',0,@APPID,@U6,1,0,'2026-06-15 11:32:00',1);
-- 明天(2026-06-17) 午餐预约 2 人
INSERT INTO bc_record (addtime, deletestatus, REVISION, app_id, user_id, bc_type, bc_channel, dintime, had_eat) VALUES
 (NOW(),'0',0,@APPID,@U2,1,1,'2026-06-17 11:30:00',0),
 (NOW(),'0',0,@APPID,@U4,1,1,'2026-06-17 11:31:00',0);
