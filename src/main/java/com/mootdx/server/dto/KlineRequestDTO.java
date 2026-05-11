package com.mootdx.server.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * K线数据请求DTO
 * 用于接收前端K线数据查询请求
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
public class KlineRequestDTO {

    /** 股票代码 */
    @NotBlank(message = "股票代码不能为空")
    private String code;

    /** 市场代码(SH/SZ) */
    @NotBlank(message = "市场代码不能为空")
    @Pattern(regexp = "^(SH|SZ)$", message = "市场代码只能是SH或SZ")
    private String market;

    /** 频率(1min/5min/15min/30min/60min/day/week/month/quarter/year) */
    @NotBlank(message = "频率不能为空")
    @Pattern(regexp = "^(1min|5min|15min|30min|60min|day|week|month|quarter|year)$", message = "频率参数不正确")
    private String freq = "day";

    /** 开始日期(YYYYMMDD) */
    private String start;

    /** 结束日期(YYYYMMDD) */
    private String end;

    /** 数量限制 */
    private Integer limit = 500;

    /** 是否强制刷新 */
    private Boolean forceRefresh = false;
}
