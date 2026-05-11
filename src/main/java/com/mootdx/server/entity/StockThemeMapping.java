package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票与题材关联表
 * 存储股票和题材的关联关系
 */
@Entity
@Table(name = "stock_theme_mapping", indexes = {
    @Index(name = "idx_theme_mapping_code", columnList = "code, market"),
    @Index(name = "idx_theme_mapping_theme", columnList = "theme_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uix_stock_theme", columnNames = {"code", "market", "theme_id"})
})
public class StockThemeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键ID

    @Column(name = "code", length = 10, nullable = false)
    private String code; // 股票代码

    @Column(name = "market", length = 10, nullable = false)
    private String market; // 市场(SH/SZ/BJ)

    @Column(name = "theme_id", length = 20, nullable = false)
    private String themeId; // 题材ID

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 创建时间

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
