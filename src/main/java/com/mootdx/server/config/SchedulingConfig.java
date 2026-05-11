package com.mootdx.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * 启用Spring定时任务支持
 * 注：实际任务调度由PersistentSchedulerService管理，从数据库加载配置
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 任务调度配置已移至TaskSchedulerConfig
    // PersistentSchedulerService负责从数据库加载和管理任务
}
