package com.mootdx.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MooTDX服务器管理应用启动类
 * 功能：启动Spring Boot应用，启用定时任务
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务，用于服务器状态检查
public class MootdxServerApplication {

    /**
     * 应用入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MootdxServerApplication.class, args);
    }
}
