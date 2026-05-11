package com.mootdx.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票持仓记录实体
 * 功能：记录股票买入、持仓、卖出信息及盈亏计算
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_position")
public class StockPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属平台ID
     */
    @Column(nullable = false)
    private Long platformId;

    // ==================== 股票信息 ====================

    /**
     * 股票代码
     */
    @Column(nullable = false, length = 10)
    private String code;

    /**
     * 市场(SH/SZ/BJ)
     */
    @Column(nullable = false, length = 10)
    private String market;

    /**
     * 股票名称
     */
    @Column(length = 50)
    private String name;

    // ==================== 买入信息 ====================

    /**
     * 买入单价
     */
    @Column(nullable = false)
    private Double buyPrice;

    /**
     * 买入数量
     */
    @Column(nullable = false)
    private Integer buyQuantity;

    /**
     * 买入金额(不含费用)
     */
    private Double buyAmount;

    /**
     * 买入日期
     */
    @Column(nullable = false)
    private LocalDate buyDate;

    /**
     * 买入时间(精确到分钟)
     */
    private LocalDateTime buyTime;

    // ==================== 当前持仓 ====================

    /**
     * 当前持有数量
     */
    private Integer currentQuantity;

    /**
     * 已卖出数量
     */
    @Builder.Default
    private Integer soldQuantity = 0;

    /**
     * 加权平均买入成本
     */
    private Double avgBuyPrice;

    // ==================== 买入费用明细 ====================

    /**
     * 买入佣金
     */
    @Builder.Default
    private Double buyCommission = 0.0;

    /**
     * 买入过户费
     */
    @Builder.Default
    private Double buyTransferFee = 0.0;

    /**
     * 融资利息
     */
    @Builder.Default
    private Double financingInterest = 0.0;

    /**
     * 总费用
     */
    @Builder.Default
    private Double totalFee = 0.0;

    /**
     * 总成本(买入金额+费用)
     */
    @Builder.Default
    private Double totalCost = 0.0;

    // ==================== 卖出信息(部分卖出时更新) ====================

    /**
     * 卖出价格
     */
    private Double sellPrice;

    /**
     * 卖出数量
     */
    private Integer sellQuantity;

    /**
     * 卖出金额
     */
    private Double sellAmount;

    /**
     * 卖出日期
     */
    private LocalDate sellDate;

    /**
     * 卖出佣金
     */
    @Builder.Default
    private Double sellCommission = 0.0;

    /**
     * 卖出印花税
     */
    @Builder.Default
    private Double sellStampTax = 0.0;

    /**
     * 卖出过户费
     */
    @Builder.Default
    private Double sellTransferFee = 0.0;

    // ==================== 盈亏计算 ====================

    /**
     * 当前价格
     */
    private Double currentPrice;

    /**
     * 市值
     */
    private Double marketValue;

    /**
     * 盈亏金额
     */
    @Builder.Default
    private Double profitAmount = 0.0;

    /**
     * 盈亏率(%)
     */
    @Builder.Default
    private Double profitRate = 0.0;

    // ==================== 状态管理 ====================

    /**
     * 状态: holding-持仓中, sold-已清仓, partial-部分卖出
     */
    @Column(length = 20)
    @Builder.Default
    private String status = "holding";

    // ==================== 交易策略 ====================

    /**
     * 交易策略
     */
    @Column(length = 100)
    private String strategy;

    /**
     * 止损价
     */
    private Double stopLossPrice;

    /**
     * 目标价
     */
    private Double targetPrice;

    // ==================== 笔记标签 ====================

    /**
     * 交易笔记
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * 标签(逗号分隔)
     */
    @Column(length = 200)
    private String tags;

    // ==================== 时间戳 ====================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 关联关系 ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platformId", insertable = false, updatable = false)
    private TradePlatform platform;
}
