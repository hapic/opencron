ALTER TABLE `t_job` ADD COLUMN `weight` INT(11) DEFAULT 0 NULL COMMENT '任务的权重' AFTER `groupId`;
ALTER TABLE `t_record` ADD COLUMN `weight` INT(11) DEFAULT 0 NULL COMMENT '任务的权重' AFTER `insertDate`;