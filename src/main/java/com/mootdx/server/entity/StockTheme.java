package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票题材信息表
 * 存储题材定义信息，支持从通达信mark.dat导入
 */
@Entity
@Table(name = "stock_theme", indexes = {
    @Index(name = "idx_theme_id", columnList = "theme_id"),
    @Index(name = "idx_theme_name", columnList = "theme_name"),
    @Index(name = "idx_theme_stock_count", columnList = "stock_count")
})
public class StockTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键ID

    @Column(name = "theme_id", length = 20, nullable = false, unique = true)
    private String themeId; // 题材ID（MD5生成）

    @Column(name = "theme_name", length = 100, nullable = false)
    private String themeName; // 题材名称

    @Column(name = "theme_keywords", length = 500)
    private String themeKeywords; // 题材关键词（逗号分隔）

    @Column(name = "description", length = 2000)
    private String description; // 题材详细描述

    @Column(name = "color", length = 20)
    private String color; // 题材显示颜色(HEX格式)

    @Column(name = "stock_count")
    private Integer stockCount = 0; // 关联股票数量

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 创建时间

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 更新时间

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getThemeKeywords() {
        return themeKeywords;
    }

    public void setThemeKeywords(String themeKeywords) {
        this.themeKeywords = themeKeywords;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
