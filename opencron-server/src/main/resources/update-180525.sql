ALTER TABLE `t_job_action_group` ADD COLUMN `alarm` INT(10) DEFAULT 0 NULL COMMENT '消息通知' AFTER `actionId`;