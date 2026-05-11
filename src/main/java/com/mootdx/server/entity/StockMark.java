package com.mootdx.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票备注标记实体类
 * 存储通达信备注数据(mark.dat)
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_mark",
        indexes = {
                @Index(name = "idx_stock_mark_code", columnList = "stock_code"),
                @Index(name = "idx_stock_mark_type", columnList = "mark_type"),
                @Index(name = "idx_stock_mark_unique", columnList = "stock_code,mark_type,mark_time", unique = true)
        })
public class StockMark {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 股票代码 */
    @Column(name = "stock_code", length = 10, nullable = false)
    private String stockCode;

    /** 标记类型：0=自定义标记, 1=最近浏览, 2=最近关注, 3=自选同列, 4=持仓股, 5=最近浏览(新) */
    @Column(name = "mark_type", nullable = false)
    private Integer markType;

    /** 标记类型描述 */
    @Column(name = "mark_type_desc", length = 50)
    private String markTypeDesc;

    /** 简字标签(2个汉字) */
    @Column(name = "tip_word", length = 10)
    private String tipWord;

    /** 提示文字(最多128个汉字) */
    @Column(name = "tip", length = 512)
    private String tip;

    /** 颜色代码 */
    @Column(name = "tip_color")
    private Integer tipColor;

    /** 颜色描述 */
    @Column(name = "color_desc", length = 20)
    private String colorDesc;

    /** 标记时间 */
    @Column(name = "mark_time", length = 20)
    private String markTime;

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
