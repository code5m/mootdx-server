package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票与标签关联表
 */
@Entity
@Table(name = "stock_tag_mapping",
    uniqueConstraints = {
        @UniqueConstraint(name = "uix_stock_tag", columnNames = {"code", "market", "tag_id"})
    },
    indexes = {
        @Index(name = "idx_mapping_code", columnList = "code, market"),
        @Index(name = "idx_mapping_tag", columnList = "tag_id"),
        @Index(name = "idx_mapping_type", columnList = "tag_type"),
        @Index(name = "idx_mapping_code_type", columnList = "code, market, tag_type")
    }
)
public class StockTagMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 10, nullable = false)
    private String code;

    @Column(name = "market", length = 10, nullable = false)
    private String market;

    @Column(name = "tag_id", length = 32, nullable = false)
    private String tagId;

    @Column(name = "tag_type", length = 20, nullable = false)
    private String tagType;

    @Column(name = "weight")
    private Integer weight = 0;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }

    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
