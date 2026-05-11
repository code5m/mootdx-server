package com.mootdx.server.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票技术指标标签表
 * 支持时间维度的技术指标信号
 */
@Entity
@Table(name = "stock_technical_tag", indexes = {
    @Index(name = "idx_tech_code", columnList = "code, market"),
    @Index(name = "idx_tech_indicator", columnList = "indicator_name"),
    @Index(name = "idx_tech_signal", columnList = "signal_type"),
    @Index(name = "idx_tech_date", columnList = "trade_date"),
    @Index(name = "idx_tech_time", columnList = "trade_time"),
    @Index(name = "idx_tech_freq", columnList = "freq"),
    @Index(name = "idx_tech_code_date", columnList = "code, market, trade_date"),
    @Index(name = "idx_tech_indicator_date", columnList = "indicator_name, trade_date"),
    @Index(name = "idx_tech_active", columnList = "is_active, confirmed")
})
public class StockTechnicalTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 10, nullable = false)
    private String code;

    @Column(name = "market", length = 10, nullable = false)
    private String market;

    @Column(name = "indicator_name", length = 50, nullable = false)
    private String indicatorName;

    @Column(name = "indicator_code", length = 20)
    private String indicatorCode;

    @Column(name = "signal_type", length = 30)
    private String signalType;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_time")
    private LocalDateTime tradeTime;

    @Column(name = "freq", length = 10)
    private String freq = "day";

    @Column(name = "indicator_value")
    private Double indicatorValue;

    @Column(name = "signal_strength")
    private Integer signalStrength = 3;

    @Column(name = "trigger_price")
    private Double triggerPrice;

    @Column(name = "trigger_volume")
    private Integer triggerVolume;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    private Integer isActive = 1;

    @Column(name = "confirmed")
    private Integer confirmed = 0;

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

    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }

    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }

    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public LocalDateTime getTradeTime() { return tradeTime; }
    public void setTradeTime(LocalDateTime tradeTime) { this.tradeTime = tradeTime; }

    public String getFreq() { return freq; }
    public void setFreq(String freq) { this.freq = freq; }

    public Double getIndicatorValue() { return indicatorValue; }
    public void setIndicatorValue(Double indicatorValue) { this.indicatorValue = indicatorValue; }

    public Integer getSignalStrength() { return signalStrength; }
    public void setSignalStrength(Integer signalStrength) { this.signalStrength = signalStrength; }

    public Double getTriggerPrice() { return triggerPrice; }
    public void setTriggerPrice(Double triggerPrice) { this.triggerPrice = triggerPrice; }

    public Integer getTriggerVolume() { return triggerVolume; }
    public void setTriggerVolume(Integer triggerVolume) { this.triggerVolume = triggerVolume; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public Integer getConfirmed() { return confirmed; }
    public void setConfirmed(Integer confirmed) { this.confirmed = confirmed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
