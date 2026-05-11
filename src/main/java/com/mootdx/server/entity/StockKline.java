package com.mootdx.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * K线数据实体类
 * 存储股票K线数据，支持多种频率（1分钟到年线）
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_kline",
        indexes = {
                @Index(name = "idx_stock_kline_query", columnList = "code,market,freq,trade_date"),
                @Index(name = "idx_stock_kline_unique", columnList = "code,market,freq,trade_date", unique = true),
                @Index(name = "idx_stock_kline_batch", columnList = "market,freq,code")  // 批量查询优化索引
        })
public class StockKline {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 股票代码 */
    @Column(name = "code", length = 10, nullable = false)
    private String code;

    /** 市场代码(SH/SZ) */
    @Column(name = "market", length = 10, nullable = false)
    private String market;

    /** 频率(1min/5min/15min/30min/60min/day/week/month/quarter/year) */
    @Column(name = "freq", length = 10, nullable = false)
    private String freq;

    /** 交易日期 */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /** 交易时间(分钟K线才有) */
    @Column(name = "trade_time")
    private LocalDateTime tradeTime;

    /** 开盘价 */
    @Column(name = "open_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal open;

    /** 最高价 */
    @Column(name = "high_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal high;

    /** 最低价 */
    @Column(name = "low_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal low;

    /** 收盘价 */
    @Column(name = "close_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal close;

    /** 成交量 */
    @Column(name = "volume", nullable = false)
    private Long volume;

    /** 成交额 */
    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 实体保存前自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 实体更新前自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
