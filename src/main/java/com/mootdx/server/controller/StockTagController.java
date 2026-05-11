package com.mootdx.server.controller;

import com.mootdx.server.common.Result;
import com.mootdx.server.entity.*;
import com.mootdx.server.service.StockTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 股票标签系统API接口
 */
@RestController
@RequestMapping("/api/stock-tags")
public class StockTagController {

    @Autowired
    private StockTagService stockTagService;

    // ==================== 标签定义管理 ====================

    @PostMapping("/tags")
    public Result<StockTag> createTag(@RequestBody Map<String, Object> request) {
        try {
            String tagId = (String) request.get("tagId");
            String tagName = (String) request.get("tagName");
            String tagType = (String) request.get("tagType");

            if (tagId == null || tagName == null || tagType == null) {
                return Result.error(400, "参数不完整: tagId, tagName, tagType 为必填项");
            }

            StockTag tag = stockTagService.createTag(tagId, tagName, tagType, request);
            return Result.success(tag);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/tags/{tagId}")
    public Result<StockTag> updateTag(@PathVariable String tagId, @RequestBody Map<String, Object> request) {
        try {
            StockTag tag = stockTagService.updateTag(tagId, request);
            return Result.success(tag);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/tags")
    public Result<List<StockTag>> getTags(
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) String parentTagId) {
        List<StockTag> tags;
        if (tagType != null && !tagType.isEmpty()) {
            if (parentTagId != null && !parentTagId.isEmpty()) {
                tags = stockTagService.getTagsByTypeAndParent(tagType, parentTagId);
            } else {
                tags = stockTagService.getTagsByType(tagType);
            }
        } else {
            tags = stockTagService.getAllTags();
        }
        return Result.success(tags);
    }

    @GetMapping("/tags/search")
    public Result<List<StockTag>> searchTags(
            @RequestParam String keyword,
            @RequestParam(required = false) String tagType) {
        List<StockTag> tags = stockTagService.searchTags(keyword, tagType);
        return Result.success(tags);
    }

    @GetMapping("/tags/{tagId}")
    public Result<StockTag> getTagDetail(@PathVariable String tagId) {
        Optional<StockTag> tag = stockTagService.getTagById(tagId);
        return tag.map(Result::success)
                .orElse(Result.error(404, "标签不存在"));
    }

    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(@PathVariable String tagId) {
        stockTagService.deleteTag(tagId);
        return Result.success(null);
    }

    // ==================== 股票-标签关联管理 ====================

    @PostMapping("/stock-tags")
    public Result<StockTagMapping> addTagToStock(@RequestBody Map<String, Object> request) {
        try {
            String code = (String) request.get("code");
            String market = (String) request.get("market");
            String tagId = (String) request.get("tagId");
            Integer weight = request.containsKey("weight") ? (Integer) request.get("weight") : 0;
            String operator = (String) request.get("operator");

            if (code == null || market == null || tagId == null) {
                return Result.error(400, "参数不完整: code, market, tagId 为必填项");
            }

            StockTagMapping mapping = stockTagService.addTagToStock(code, market.toUpperCase(), tagId, weight, operator);
            return Result.success(mapping);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/stock-tags")
    public Result<Void> removeTagFromStock(
            @RequestParam String code,
            @RequestParam String market,
            @RequestParam String tagId,
            @RequestParam(required = false) String operator) {
        boolean success = stockTagService.removeTagFromStock(code, market.toUpperCase(), tagId, operator);
        if (success) {
            return Result.success(null);
        }
        return Result.error(404, "标签关联不存在");
    }

    @GetMapping("/stock/{market}/{code}")
    public Result<List<Map<String, Object>>> getStockTags(
            @PathVariable String market,
            @PathVariable String code,
            @RequestParam(required = false) String tagType) {
        if (!Arrays.asList("SH", "SZ", "BJ").contains(market.toUpperCase())) {
            return Result.error(400, "市场代码必须是 SH、SZ 或 BJ");
        }
        List<Map<String, Object>> tags = stockTagService.getStockTags(code, market.toUpperCase(), tagType);
        return Result.success(tags);
    }

    @GetMapping("/tag/{tagId}/stocks")
    public Result<Map<String, Object>> getTagStocks(
            @PathVariable String tagId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        List<Map<String, Object>> stocks = stockTagService.getStocksByTag(tagId, limit);
        Map<String, Object> result = new HashMap<>();
        result.put("count", stocks.size());
        result.put("data", stocks);
        return Result.success(result);
    }

    // ==================== 技术指标标签管理 ====================

    @PostMapping("/technical-tags")
    public Result<StockTechnicalTag> addTechnicalTag(@RequestBody Map<String, Object> request) {
        try {
            String code = (String) request.get("code");
            String market = (String) request.get("market");
            String indicatorName = (String) request.get("indicatorName");
            String signalType = (String) request.get("signalType");
            String tradeDateStr = (String) request.get("tradeDate");

            if (code == null || market == null || indicatorName == null || tradeDateStr == null) {
                return Result.error(400, "参数不完整");
            }

            LocalDate tradeDate = LocalDate.parse(tradeDateStr);

            StockTechnicalTag tag = stockTagService.addTechnicalTag(
                    code, market.toUpperCase(), indicatorName, tradeDate, signalType, request);
            return Result.success(tag);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/technical-tags")
    public Result<List<StockTechnicalTag>> getTechnicalTags(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String indicatorName,
            @RequestParam(required = false) String signalType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Integer isActive,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {

        String upperMarket = market != null ? market.toUpperCase() : null;
        List<StockTechnicalTag> tags = stockTagService.getTechnicalTags(
                code, upperMarket, indicatorName, signalType, tradeDate, startDate, endDate, isActive, limit);
        return Result.success(tags);
    }

    @GetMapping("/technical-signals")
    public Result<Map<String, Object>> getStocksByTechnicalSignal(
            @RequestParam String indicatorName,
            @RequestParam String signalType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(required = false, defaultValue = "1") Integer minStrength) {

        List<Map<String, Object>> stocks = stockTagService.getStocksByTechnicalSignal(
                indicatorName, signalType, tradeDate, minStrength);

        Map<String, Object> result = new HashMap<>();
        result.put("count", stocks.size());
        result.put("date", tradeDate.toString());
        result.put("data", stocks);
        return Result.success(result);
    }

    // ==================== 行业分类管理 ====================

    @PostMapping("/industries")
    public Result<StockIndustry> setStockIndustry(@RequestBody Map<String, Object> request) {
        try {
            String code = (String) request.get("code");
            String market = (String) request.get("market");
            String standard = (String) request.get("standard");
            String level1Code = (String) request.get("level1Code");
            String level1Name = (String) request.get("level1Name");

            if (code == null || market == null || standard == null || level1Code == null || level1Name == null) {
                return Result.error(400, "参数不完整");
            }

            StockIndustry industry = stockTagService.setStockIndustry(
                    code, market.toUpperCase(), standard, level1Code, level1Name, request);
            return Result.success(industry);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/industries/{market}/{code}")
    public Result<List<StockIndustry>> getStockIndustries(
            @PathVariable String market,
            @PathVariable String code) {
        List<StockIndustry> industries = stockTagService.getStockIndustries(code, market.toUpperCase());
        return Result.success(industries);
    }

    // ==================== 统计信息 ====================

    @GetMapping("/stats")
    public Result<Map<String, Long>> getTagStats() {
        return Result.success(stockTagService.getTagStats());
    }
}
