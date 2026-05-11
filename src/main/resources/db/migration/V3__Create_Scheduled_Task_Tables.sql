-- 定时任务配置表
CREATE TABLE IF NOT EXISTS scheduled_task (
    task_id VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '任务ID',
    task_name VARCHAR(128) NOT NULL COMMENT '任务名称',
    description VARCHAR(512) COMMENT '任务描述',
    task_type VARCHAR(20) NOT NULL DEFAULT 'FIXED_RATE' COMMENT '任务类型：CRON, FIXED_RATE, FIXED_DELAY',
    cron_expression VARCHAR(128) COMMENT 'Cron表达式',
    fixed_rate BIGINT COMMENT '固定频率/延迟的毫秒数',
    initial_delay BIGINT DEFAULT 0 COMMENT '初始延迟（毫秒）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    target_bean VARCHAR(128) NOT NULL COMMENT '目标Bean名称',
    target_method VARCHAR(128) NOT NULL COMMENT '目标方法名',
    task_params VARCHAR(2048) COMMENT '任务参数（JSON格式）',
    last_execute_time TIMESTAMP COMMENT '最后一次执行时间',
    last_execute_result VARCHAR(20) COMMENT '最后一次执行结果：SUCCESS, FAILED',
    last_execute_message VARCHAR(1024) COMMENT '最后一次执行结果消息',
    execute_count INT NOT NULL DEFAULT 0 COMMENT '执行次数统计',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功次数统计',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败次数统计',
    concurrent_policy VARCHAR(20) DEFAULT 'CONCURRENT' COMMENT '并发策略：CONCURRENT, DISCARD, BLOCK, REPLACE',
    timeout BIGINT DEFAULT 0 COMMENT '超时时间（毫秒），0表示不超时',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    retry_interval BIGINT DEFAULT 5000 COMMENT '重试间隔（毫秒）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='定时任务配置表';

-- 任务执行日志表
CREATE TABLE IF NOT EXISTS task_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    task_name VARCHAR(128) COMMENT '任务名称',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：RUNNING, SUCCESS, FAILED, TIMEOUT',
    start_time TIMESTAMP NOT NULL COMMENT '开始执行时间',
    end_time TIMESTAMP COMMENT '结束执行时间',
    duration_ms BIGINT COMMENT '执行耗时（毫秒）',
    result_message VARCHAR(2048) COMMENT '执行结果消息',
    exception_info VARCHAR(4000) COMMENT '异常信息',
    task_params VARCHAR(2048) COMMENT '任务参数（JSON格式）',
    result_data VARCHAR(4000) COMMENT '执行结果数据（JSON格式）',
    server_host VARCHAR(128) COMMENT '服务器主机名',
    server_ip VARCHAR(64) COMMENT '服务器IP地址',
    trigger_type VARCHAR(20) DEFAULT 'SCHEDULED' COMMENT '触发类型：SCHEDULED, MANUAL, RETRY',
    retry_times INT DEFAULT 0 COMMENT '重试次数',
    execute_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间'
) COMMENT='定时任务执行日志表';

-- 创建索引
CREATE INDEX idx_task_id ON task_execution_log(task_id);
CREATE INDEX idx_execute_time ON task_execution_log(execute_time);
CREATE INDEX idx_status ON task_execution_log(status);
CREATE INDEX idx_start_time ON task_execution_log(start_time);

-- 初始化默认定时任务数据
-- 服务器状态检查任务
INSERT INTO scheduled_task (
    task_id, task_name, description, task_type, fixed_rate, 
    initial_delay, enabled, target_bean, target_method,
    concurrent_policy, timeout, retry_count, retry_interval
) VALUES (
    'serverStatusCheck', 
    '服务器状态检查', 
    '定时检查所有TDX服务器的状态，更新服务器可用性',
    'FIXED_RATE',
    60000,
    10000,
    TRUE,
    'serverStatusCheckScheduler',
    'checkAllServersStatus',
    'CONCURRENT',
    30000,
    3,
    5000
) ON DUPLICATE KEY UPDATE task_name = task_name;

-- 数据表监控任务
INSERT INTO scheduled_task (
    task_id, task_name, description, task_type, fixed_rate, 
    initial_delay, enabled, target_bean, target_method,
    concurrent_policy, timeout, retry_count, retry_interval
) VALUES (
    'tableMonitor', 
    '数据表监控', 
    '定时监控数据表状态，检查数据更新情况',
    'FIXED_RATE',
    300000,
    30000,
    TRUE,
    'tableMonitorScheduler',
    'monitorTables',
    'CONCURRENT',
    60000,
    2,
    10000
) ON DUPLICATE KEY UPDATE task_name = task_name;
