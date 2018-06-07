CREATE TABLE `t_alarm` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `monitor_id` int(20) NOT NULL COMMENT '监控对应的ID',
  `monitor_type` int(10) NOT NULL COMMENT '监控的类型',
  `alarm_code` int(10) NOT NULL COMMENT '告警类型码(1- 2- 4- 8-)',
  `alarm_way` int(10) NOT NULL COMMENT '告警方式码(1-手机 2-邮箱 4-钉钉 8-微信)',
  `alarm_person` varchar(256) DEFAULT NULL COMMENT '告警接收人',
  `insert_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '插入时间',
  `update_date` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `t_job` CHANGE `warning` `alarmCode` INT(10) NULL COMMENT '通知类型码';
#ALTER TABLE `t_job` DROP COLUMN `emailAddress`, DROP COLUMN `flowId`, DROP COLUMN `mobiles`;

ALTER TABLE `t_job_action_group` ADD COLUMN `param` VARCHAR(128) NULL COMMENT '手动执行的参数';
ALTER TABLE `t_config` ADD COLUMN `ddToken` varchar(128) DEFAULT NULL COMMENT '钉钉告警token';