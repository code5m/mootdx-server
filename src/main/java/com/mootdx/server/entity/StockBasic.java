package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票基础信息表
 * 存储股票的基本信息
 */
@Entity
@Table(name = "stock_basic", indexes = {
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_market", columnList = "market"),
    @Index(name = "idx_stock_type", columnList = "stock_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uix_code_market", columnNames = {"code", "market"})
})
public class StockBasic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键ID

    @Column(name = "code", length = 10, nullable = false)
    private String code; // 股票代码

    @Column(name = "name", length = 50)
    private String name; // 股票名称

    @Column(name = "market", length = 10)
    private String market; // 市场(SH-上海/SZ-深圳/BJ-北京)

    @Column(name = "industry", length = 50)
    private String industry; // 所属行业

    @Column(name = "stock_type", length = 20)
    private String stockType = "stock"; // 股票类型

    @Column(name = "total_capital")
    private Float totalCapital; // 总股本(万股)

    @Column(name = "circulating_capital")
    private Float circulatingCapital; // 流通股本(万股)

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getStockType() {
        return stockType;
    }

    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    public Float getTotalCapital() {
        return totalCapital;
    }

    public void setTotalCapital(Float totalCapital) {
        this.totalCapital = totalCapital;
    }

    public Float getCirculatingCapital() {
        return circulatingCapital;
    }

    public void setCirculatingCapital(Float circulatingCapital) {
        this.circulatingCapital = circulatingCapital;
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
