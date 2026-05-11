package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票题材详细描述表
 * 存储每只股票的题材详细描述文本
 */
@Entity
@Table(name = "stock_theme_detail", indexes = {
    @Index(name = "idx_detail_code", columnList = "code, market")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uix_theme_detail", columnNames = {"code", "market"})
})
public class StockThemeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键ID

    @Column(name = "code", length = 10, nullable = false)
    private String code; // 股票代码

    @Column(name = "market", length = 10, nullable = false)
    private String market; // 市场(SH/SZ/BJ)

    @Column(name = "detail_text", length = 4000)
    private String detailText; // 题材详细描述文本

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

    public String getDetailText() {
        return detailText;
    }

    public void setDetailText(String detailText) {
        this.detailText = detailText;
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
