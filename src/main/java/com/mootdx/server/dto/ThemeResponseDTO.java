package com.mootdx.server.dto;

/**
 * 题材响应DTO
 */
public class ThemeResponseDTO {

    private String themeId; // 题材ID
    private String themeName; // 题材名称
    private String themeKeywords; // 题材关键词
    private Integer stockCount; // 关联股票数量
    private String color; // 显示颜色

    public ThemeResponseDTO() {
    }

    public ThemeResponseDTO(String themeId, String themeName, String themeKeywords, Integer stockCount, String color) {
        this.themeId = themeId;
        this.themeName = themeName;
        this.themeKeywords = themeKeywords;
        this.stockCount = stockCount;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
