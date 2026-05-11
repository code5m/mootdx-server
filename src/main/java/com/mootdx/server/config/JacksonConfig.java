package com.mootdx.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Jackson配置类
 * 功能：配置JSON序列化，支持Java 8日期时间类型
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置ObjectMapper
     * 支持Java 8日期时间类型的序列化
     *
     * @return ObjectMapper实例
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册JavaTimeModule以支持Java 8日期时间
        mapper.registerModule(new JavaTimeModule());

        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 设置日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        mapper.setDateFormat(dateFormat);

        // 设置时区
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        return mapper;
    }
}
