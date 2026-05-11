package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票行业分类表
 * 支持多种行业分类标准：申万、中信、证监会、Wind
 */
@Entity
@Table(name = "stock_industry",
    uniqueConstraints = {
        @UniqueConstraint(name = "uix_stock_industry", columnNames = {"code", "market", "standard"})
    },
    indexes = {
        @Index(name = "idx_industry_code", columnList = "code, market"),
        @Index(name = "idx_industry_std", columnList = "standard"),
        @Index(name = "idx_industry_l1", columnList = "industry_level1_code"),
        @Index(name = "idx_industry_l2", columnList = "industry_level2_code"),
        @Index(name = "idx_industry_l3", columnList = "industry_level3_code")
    }
)
public class StockIndustry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 10, nullable = false)
    private String code;

    @Column(name = "market", length = 10, nullable = false)
    private String market;

    @Column(name = "standard", length = 20, nullable = false)
    private String standard = "sw";

    @Column(name = "industry_level1_code", length = 20)
    private String industryLevel1Code;

    @Column(name = "industry_level1_name", length = 50)
    private String industryLevel1Name;

    @Column(name = "industry_level2_code", length = 20)
    private String industryLevel2Code;

    @Column(name = "industry_level2_name", length = 50)
    private String industryLevel2Name;

    @Column(name = "industry_level3_code", length = 20)
    private String industryLevel3Code;

    @Column(name = "industry_level3_name", length = 50)
    private String industryLevel3Name;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }

    public String getIndustryLevel1Code() { return industryLevel1Code; }
    public void setIndustryLevel1Code(String industryLevel1Code) { this.industryLevel1Code = industryLevel1Code; }

    public String getIndustryLevel1Name() { return industryLevel1Name; }
    public void setIndustryLevel1Name(String industryLevel1Name) { this.industryLevel1Name = industryLevel1Name; }

    public String getIndustryLevel2Code() { return industryLevel2Code; }
    public void setIndustryLevel2Code(String industryLevel2Code) { this.industryLevel2Code = industryLevel2Code; }

    public String getIndustryLevel2Name() { return industryLevel2Name; }
    public void setIndustryLevel2Name(String industryLevel2Name) { this.industryLevel2Name = industryLevel2Name; }

    public String getIndustryLevel3Code() { return industryLevel3Code; }
    public void setIndustryLevel3Code(String industryLevel3Code) { this.industryLevel3Code = industryLevel3Code; }

    public String getIndustryLevel3Name() { return industryLevel3Name; }
    public void setIndustryLevel3Name(String industryLevel3Name) { this.industryLevel3Name = industryLevel3Name; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
