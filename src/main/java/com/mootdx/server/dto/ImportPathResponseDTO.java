package com.mootdx.server.dto;

/**
 * 导入路径响应DTO
 */
public class ImportPathResponseDTO {

    private String defaultPath; // 默认路径
    private boolean exists; // 文件是否存在
    private String message; // 消息

    public ImportPathResponseDTO() {
    }

    public ImportPathResponseDTO(String defaultPath, boolean exists, String message) {
        this.defaultPath = defaultPath;
        this.exists = exists;
        this.message = message;
    }

    // Getters and Setters
    public String getDefaultPath() {
        return defaultPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
