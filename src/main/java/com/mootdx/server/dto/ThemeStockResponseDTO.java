package com.mootdx.server.dto;

import java.util.List;

/**
 * 题材股票列表响应DTO
 */
public class ThemeStockResponseDTO {

    private String themeId; // 题材ID
    private String themeName; // 题材名称
    private String themeKeywords; // 题材关键词
    private Integer stockCount; // 股票数量
    private List<StockThemeInfoDTO> stocks; // 股票列表

    public ThemeStockResponseDTO() {
    }

    // Getters and Setters
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

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public List<StockThemeInfoDTO> getStocks() {
        return stocks;
    }

    public void setStocks(List<StockThemeInfoDTO> stocks) {
        this.stocks = stocks;
    }
}
