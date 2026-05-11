package com.mootdx.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * K线数据传输对象
 * 用于前后端数据交互
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineDataDTO {

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 交易时间(分钟K线才有) */
    private LocalDateTime tradeTime;

    /** 开盘价 */
    private BigDecimal open;

    /** 最高价 */
    private BigDecimal high;

    /** 最低价 */
    private BigDecimal low;

    /** 收盘价 */
    private BigDecimal close;

    /** 成交量 */
    private Long volume;

    /** 成交额 */
    private BigDecimal amount;
}
