package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 股票标签操作历史记录表
 */
@Entity
@Table(name = "stock_tag_history", indexes = {
    @Index(name = "idx_history_code", columnList = "code, market"),
    @Index(name = "idx_history_tag", columnList = "tag_id"),
    @Index(name = "idx_history_time", columnList = "created_at")
})
public class StockTagHistory {

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

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "source", length = 50)
    private String source;

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

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
