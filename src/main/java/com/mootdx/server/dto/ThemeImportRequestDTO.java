package com.mootdx.server.dto;

import java.util.List;

/**
 * 题材导入请求DTO
 */
public class ThemeImportRequestDTO {

    private List<String> filePaths; // mark.dat文件路径列表

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }
}
