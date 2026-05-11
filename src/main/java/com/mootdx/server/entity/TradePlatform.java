package com.mootdx.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易平台/券商账户实体
 * 功能：管理多个券商账户及其费率配置
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trade_platform")
public class TradePlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 平台名称(如:华泰证券、东方财富)
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 账户号码
     */
    @Column(length = 50)
    private String accountNo;

    /**
     * 账户类型: stock-股票, margin-融资融券, hk-港股通
     */
    @Column(length = 20)
    @Builder.Default
    private String accountType = "stock";

    // ==================== 买入费率配置 ====================

    /**
     * 买入佣金费率(默认万2.5)
     */
    @Builder.Default
    private Double buyCommissionRate = 0.00025;

    /**
     * 买入最低佣金
     */
    @Builder.Default
    private Double buyMinCommission = 5.0;

    // ==================== 卖出费率配置 ====================

    /**
     * 卖出佣金费率(默认万2.5)
     */
    @Builder.Default
    private Double sellCommissionRate = 0.00025;

    /**
     * 卖出最低佣金
     */
    @Builder.Default
    private Double sellMinCommission = 5.0;

    /**
     * 印花税费率(卖出时收取,默认千1)
     */
    @Builder.Default
    private Double stampTaxRate = 0.001;

    /**
     * 过户费率(默认十万分之2)
     */
    @Builder.Default
    private Double transferFeeRate = 0.00002;

    // ==================== 融资费用(融资融券账户) ====================

    /**
     * 融资年利率(默认8%)
     */
    @Builder.Default
    private Double financingRate = 0.08;

    /**
     * 融券年利率(默认10%)
     */
    @Builder.Default
    private Double shortSellingRate = 0.10;

    // ==================== 账户状态 ====================

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 是否默认平台
     */
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * 排序顺序
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 备注说明
     */
    @Column(length = 500)
    private String remark;

    // ==================== 时间戳 ====================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 关联关系 ====================

    @OneToMany(mappedBy = "platform", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StockPosition> positions = new ArrayList<>();
}
