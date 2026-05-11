package com.mootdx.server.dto;

/**
 * 题材导入响应DTO
 */
public class ThemeImportResponseDTO {

    private boolean success; // 是否成功
    private String message; // 消息
    private int themesAdded; // 新增题材数量
    private int themesUpdated; // 更新题材数量
    private int mappingsAdded; // 新增关联数量
    private int detailsAdded; // 新增详细描述数量
    private int detailsUpdated; // 更新详细描述数量

    public ThemeImportResponseDTO() {
    }

    public ThemeImportResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getThemesAdded() {
        return themesAdded;
    }

    public void setThemesAdded(int themesAdded) {
        this.themesAdded = themesAdded;
    }

    public int getThemesUpdated() {
        return themesUpdated;
    }

    public void setThemesUpdated(int themesUpdated) {
        this.themesUpdated = themesUpdated;
    }

    public int getMappingsAdded() {
        return mappingsAdded;
    }

    public void setMappingsAdded(int mappingsAdded) {
        this.mappingsAdded = mappingsAdded;
    }

    public int getDetailsAdded() {
        return detailsAdded;
    }

    public void setDetailsAdded(int detailsAdded) {
        this.detailsAdded = detailsAdded;
    }

    public int getDetailsUpdated() {
        return detailsUpdated;
    }

    public void setDetailsUpdated(int detailsUpdated) {
        this.detailsUpdated = detailsUpdated;
    }
}
