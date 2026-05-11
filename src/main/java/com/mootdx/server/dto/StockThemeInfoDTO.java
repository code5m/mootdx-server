package com.mootdx.server.dto;

import java.util.List;

/**
 * 股票题材信息DTO
 */
public class StockThemeInfoDTO {

    private String code; // 股票代码
    private String market; // 市场
    private String name; // 股票名称
    private String industry; // 所属行业
    private List<ThemeResponseDTO> themes; // 题材列表
    private String detail; // 详细描述

    public StockThemeInfoDTO() {
    }

    // Getters and Setters
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public List<ThemeResponseDTO> getThemes() {
        return themes;
    }

    public void setThemes(List<ThemeResponseDTO> themes) {
        this.themes = themes;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
