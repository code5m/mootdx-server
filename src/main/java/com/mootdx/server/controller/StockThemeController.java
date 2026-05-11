package com.mootdx.server.controller;

import com.mootdx.server.common.Result;
import com.mootdx.server.dto.*;
import com.mootdx.server.service.StockThemeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 股票题材检索API控制器
 * 提供题材搜索、导入、查询等功能
 */
@RestController
@RequestMapping("/api/themes")
public class StockThemeController {

    private static final Logger logger = LoggerFactory.getLogger(StockThemeController.class);

    @Autowired
    private StockThemeService stockThemeService;

    /**
     * 从mark.dat文件导入题材数据
     *
     * @param request 导入请求，包含文件路径列表
     * @return 导入结果
     */
    @PostMapping("/import")
    public Result<ThemeImportResponseDTO> importThemes(@RequestBody ThemeImportRequestDTO request) {
        logger.info("[API调用] POST /api/themes/import");

        try {
            List<String> filePaths = request != null ? request.getFilePaths() : null;
            ThemeImportResponseDTO result = stockThemeService.importThemes(filePaths);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("[API错误] 导入题材失败", e);
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认导入路径
     *
     * @return 默认路径信息
     */
    @GetMapping("/import-path")
    public Result<ImportPathResponseDTO> getImportPath() {
        logger.info("[API调用] GET /api/themes/import-path");

        try {
            ImportPathResponseDTO result = stockThemeService.getDefaultImportPath();
            return Result.success(result);
        } catch (Exception e) {
            logger.error("[API错误] 获取导入路径失败", e);
            return Result.error(500, "获取路径失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有题材列表
     *
     * @return 题材列表
     */
    @GetMapping("/list")
    public Result<List<ThemeResponseDTO>> getThemeList() {
        logger.info("[API调用] GET /api/themes/list");

        try {
            List<ThemeResponseDTO> themes = stockThemeService.getAllThemes();
            return Result.success(themes);
        } catch (Exception e) {
            logger.error("[API错误] 获取题材列表失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门题材（前20个）
     *
     * @return 热门题材列表
     */
    @GetMapping("/hot")
    public Result<List<ThemeResponseDTO>> getHotThemes() {
        logger.info("[API调用] GET /api/themes/hot");

        try {
            List<ThemeResponseDTO> themes = stockThemeService.getHotThemes();
            return Result.success(themes);
        } catch (Exception e) {
            logger.error("[API错误] 获取热门题材失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 搜索题材
     *
     * @param keyword 搜索关键词
     * @return 匹配的题材列表
     */
    @GetMapping("/search")
    public Result<List<ThemeResponseDTO>> searchThemes(@RequestParam String keyword) {
        logger.info("[API调用] GET /api/themes/search, keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error(400, "搜索关键词不能为空");
        }

        try {
            List<ThemeResponseDTO> themes = stockThemeService.searchThemes(keyword);
            return Result.success(themes);
        } catch (Exception e) {
            logger.error("[API错误] 搜索题材失败", e);
            return Result.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定题材下的股票列表
     *
     * @param themeId 题材ID
     * @return 股票列表
     */
    @GetMapping("/{themeId}/stocks")
    public Result<ThemeStockResponseDTO> getThemeStocks(@PathVariable String themeId) {
        logger.info("[API调用] GET /api/themes/{}/stocks", themeId);

        try {
            ThemeStockResponseDTO result = stockThemeService.getThemeStocks(themeId);
            if (result == null) {
                return Result.error(404, "题材不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            logger.error("[API错误] 获取题材股票失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取无题材的股票列表
     *
     * @return 无题材股票列表
     */
    @GetMapping("/no-theme/stocks")
    public Result<ThemeStockResponseDTO> getNoThemeStocks() {
        logger.info("[API调用] GET /api/themes/no-theme/stocks");

        try {
            ThemeStockResponseDTO result = stockThemeService.getStocksWithoutTheme();
            return Result.success(result);
        } catch (Exception e) {
            logger.error("[API错误] 获取无题材股票失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定股票的题材信息
     *
     * @param market 市场代码(SH/SZ/BJ)
     * @param code 股票代码
     * @return 股票题材信息
     */
    @GetMapping("/stock/{market}/{code}")
    public Result<StockThemeInfoDTO> getStockThemes(
            @PathVariable String market,
            @PathVariable String code) {
        logger.info("[API调用] GET /api/themes/stock/{}/{}", market, code);

        // 验证市场代码
        String upperMarket = market.toUpperCase();
        if (!Arrays.asList("SH", "SZ", "BJ").contains(upperMarket)) {
            return Result.error(400, "市场代码必须是 SH、SZ 或 BJ");
        }

        try {
            StockThemeInfoDTO result = stockThemeService.getStockThemes(code, upperMarket);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("[API错误] 获取股票题材失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }
}
