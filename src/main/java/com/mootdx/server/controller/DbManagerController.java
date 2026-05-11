package com.mootdx.server.controller;

import com.mootdx.server.common.Result;
import com.mootdx.server.dto.*;
import com.mootdx.server.service.DbManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据库管理API接口
 * 提供表管理、SQL执行、历史记录等功能
 */
@RestController
@RequestMapping("/api/db-manager")
public class DbManagerController {

    @Autowired
    private DbManagerService dbManagerService;

    // ==================== 表管理 ====================

    /**
     * 获取所有数据库表信息
     */
    @GetMapping("/tables")
    public Result<TableListResponseDTO> getAllTables() {
        try {
            TableListResponseDTO response = dbManagerService.getAllTables();
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(500, "获取表列表失败: " + e.getMessage());
        }
    }

    /**
     * 刷新表列表并检测新增表
     */
    @PostMapping("/tables/refresh")
    public Result<TableListResponseDTO> refreshTables() {
        try {
            TableListResponseDTO response = dbManagerService.refreshTables();
            if (Boolean.TRUE.equals(response.getHasNewTables())) {
                return Result.success("刷新成功，发现 " + response.getNewTableCount() + " 个新表", response);
            }
            return Result.success("刷新成功，暂无新表", response);
        } catch (Exception e) {
            return Result.error(500, "刷新表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取新增表列表（不更新已知表列表）
     */
    @GetMapping("/tables/new")
    public Result<Map<String, Object>> getNewTables() {
        try {
            List<String> newTables = dbManagerService.getNewTables();
            Map<String, Object> result = new HashMap<>();
            result.put("newTables", newTables);
            result.put("count", newTables.size());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取新表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表结构信息
     */
    @GetMapping("/tables/{tableName}/structure")
    public Result<TableStructureDTO> getTableStructure(@PathVariable String tableName) {
        try {
            TableStructureDTO structure = dbManagerService.getTableStructure(tableName);
            return Result.success(structure);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取表数据（分页）
     */
    @GetMapping("/tables/{tableName}/data")
    public Result<TableDataResponseDTO> getTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize,
            @RequestParam(required = false) String sortColumn,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        try {
            // 参数校验
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 50;
            if (pageSize > 1000) pageSize = 1000;

            TableDataResponseDTO data = dbManagerService.getTableData(
                    tableName, page, pageSize, sortColumn, sortOrder);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ==================== SQL执行 ====================

    /**
     * 执行SQL语句
     */
    @PostMapping("/execute")
    public Result<SqlExecuteResultDTO> executeSql(@RequestBody SqlExecuteRequestDTO request) {
        try {
            if (request.getSql() == null || request.getSql().trim().isEmpty()) {
                return Result.error(400, "SQL语句不能为空");
            }

            SqlExecuteResultDTO result = dbManagerService.executeSql(
                    request.getSql().trim(), request.getParams());

            if (Boolean.TRUE.equals(result.getSuccess())) {
                return Result.success(result.getMessage(), result);
            } else {
                return Result.error(400, result.getMessage());
            }
        } catch (Exception e) {
            return Result.error(500, "SQL执行失败: " + e.getMessage());
        }
    }

    /**
     * 批量执行SQL语句
     */
    @PostMapping("/execute-batch")
    public Result<List<SqlExecuteResultDTO>> executeBatchSql(@RequestBody List<String> sqls) {
        try {
            if (sqls == null || sqls.isEmpty()) {
                return Result.error(400, "SQL语句列表不能为空");
            }

            List<SqlExecuteResultDTO> results = dbManagerService.executeBatchSql(sqls);
            return Result.success(results);
        } catch (Exception e) {
            return Result.error(500, "批量执行失败: " + e.getMessage());
        }
    }

    // ==================== SQL历史记录 ====================

    /**
     * 获取SQL执行历史记录
     */
    @GetMapping("/sql-history")
    public Result<Map<String, Object>> getSqlHistory(
            @RequestParam(defaultValue = "50") Integer limit) {
        try {
            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;

            List<SqlHistoryItemDTO> history = dbManagerService.getSqlHistory(limit);
            Map<String, Object> result = new HashMap<>();
            result.put("history", history);
            result.put("total", history.size());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 清空SQL执行历史记录
     */
    @DeleteMapping("/sql-history")
    public Result<Void> clearSqlHistory() {
        try {
            dbManagerService.clearSqlHistory();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "清空历史记录失败: " + e.getMessage());
        }
    }

    // ==================== SQL模板 ====================

    /**
     * 获取所有SQL模板
     */
    @GetMapping("/sql-templates")
    public Result<Map<String, Object>> getSqlTemplates(
            @RequestParam(required = false) String category) {
        try {
            List<SqlTemplateDTO> templates = dbManagerService.getSqlTemplates(category);

            // 提取分类
            Set<String> categories = new LinkedHashSet<>();
            for (SqlTemplateDTO template : templates) {
                categories.add(template.getCategory());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("templates", templates);
            result.put("categories", new ArrayList<>(categories));
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取SQL模板失败: " + e.getMessage());
        }
    }

    /**
     * 获取SQL模板分类列表
     */
    @GetMapping("/sql-templates/categories")
    public Result<List<String>> getSqlTemplateCategories() {
        try {
            List<String> categories = dbManagerService.getSqlTemplateCategories();
            return Result.success(categories);
        } catch (Exception e) {
            return Result.error(500, "获取模板分类失败: " + e.getMessage());
        }
    }

    // ==================== 数据导出 ====================

    /**
     * 导出表数据
     */
    @GetMapping("/tables/{tableName}/export")
    public Result<Map<String, Object>> exportTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            if (!Arrays.asList("csv", "json", "sql").contains(format.toLowerCase())) {
                return Result.error(400, "不支持的导出格式: " + format);
            }

            byte[] data = dbManagerService.exportTableData(tableName, format);
            String base64Data = Base64.getEncoder().encodeToString(data);

            Map<String, Object> result = new HashMap<>();
            result.put("tableName", tableName);
            result.put("format", format);
            result.put("data", base64Data);
            result.put("filename", tableName + "_" + System.currentTimeMillis() + "." + format);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "导出失败: " + e.getMessage());
        }
    }
}
