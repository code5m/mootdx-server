package com.mootdx.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * TDX服务器配置属性类
 * 从application.yml中读取mootdx.tdx前缀的配置项
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mootdx.tdx")
public class TdxServerProperties {

    /** TDX服务器地址（为空时自动选择） */
    private String host = "";

    /** TDX服务器端口（默认7709） */
    private int port = 7709;

    /** 是否启用心跳（默认true） */
    private boolean heartbeat = true;

    /** 默认服务器列表（自动选择时使用） */
    private List<String> defaultServers;

    /** 初始化服务器列表（用于数据库初始化） */
    private List<ServerConfig> initServers;

    /**
     * 服务器配置内部类
     */
    @Data
    public static class ServerConfig {
        /** 服务器名称 */
        private String name;
        /** 服务器地址 */
        private String host;
        /** 服务器端口 */
        private int port = 7709;
        /** 服务器位置/运营商 */
        private String location;
        /** 权重 */
        private int weight = 100;
    }
}
