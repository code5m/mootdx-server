package com.mootdx.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票板块实体类
 * 存储通达信板块信息，包括系统板块和自定义板块
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_block",
        indexes = {
                @Index(name = "idx_stock_block_code", columnList = "block_code"),
                @Index(name = "idx_stock_block_type", columnList = "block_type"),
                @Index(name = "idx_stock_block_unique", columnList = "block_code,stock_code", unique = true)
        })
public class StockBlock {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 板块代码 */
    @Column(name = "block_code", length = 20, nullable = false)
    private String blockCode;

    /** 板块名称 */
    @Column(name = "block_name", length = 100, nullable = false)
    private String blockName;

    /** 板块类型：system=系统板块, custom=自定义板块 */
    @Column(name = "block_type", length = 20, nullable = false)
    private String blockType;

    /** 股票代码 */
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

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
