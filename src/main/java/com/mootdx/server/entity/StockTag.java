package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票标签定义表
 * 支持题材、行业、概念、技术指标等多种标签类型
 */
@Entity
@Table(name = "stock_tag", indexes = {
    @Index(name = "idx_tag_id", columnList = "tag_id"),
    @Index(name = "idx_tag_type", columnList = "tag_type"),
    @Index(name = "idx_tag_name", columnList = "tag_name"),
    @Index(name = "idx_tag_type_name", columnList = "tag_type, tag_name"),
    @Index(name = "idx_parent_tag", columnList = "parent_tag_id")
})
public class StockTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_id", length = 32, nullable = false, unique = true)
    private String tagId;

    @Column(name = "tag_name", length = 100, nullable = false)
    private String tagName;

    @Column(name = "tag_type", length = 20, nullable = false)
    private String tagType = "theme";

    @Column(name = "tag_level")
    private Integer tagLevel = 1;

    @Column(name = "parent_tag_id", length = 32)
    private String parentTagId;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "stock_count")
    private Integer stockCount = 0;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "source_code", length = 50)
    private String sourceCode;

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

    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }

    public Integer getTagLevel() { return tagLevel; }
    public void setTagLevel(Integer tagLevel) { this.tagLevel = tagLevel; }

    public String getParentTagId() { return parentTagId; }
    public void setParentTagId(String parentTagId) { this.parentTagId = parentTagId; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getStockCount() { return stockCount; }
    public void setStockCount(Integer stockCount) { this.stockCount = stockCount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
