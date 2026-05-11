package com.mootdx.server.controller;

import com.mootdx.server.common.Result;
import com.mootdx.server.dto.StockPositionDTO;
import com.mootdx.server.dto.TradePlatformDTO;
import com.mootdx.server.service.StockNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 持仓笔记Controller
 * 功能：提供持仓笔记API接口
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/stock-notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StockNoteController {

    private final StockNoteService stockNoteService;

    // ==================== 平台管理API ====================

    /**
     * 创建交易平台
     * POST /api/stock-notes/platforms
     *
     * @param request 创建请求
     * @return 平台信息
     */
    @PostMapping("/platforms")
    public Result<TradePlatformDTO.Response> createPlatform(
            @Valid @RequestBody TradePlatformDTO.CreateRequest request) {
        log.info("[createPlatform] 创建交易平台: name={}", request.getName());
        TradePlatformDTO.Response response = stockNoteService.createPlatform(request);
        return Result.success(response);
    }

    /**
     * 获取平台列表
     * GET /api/stock-notes/platforms
     *
     * @param activeOnly 是否只返回启用的平台
     * @return 平台列表
     */
    @GetMapping("/platforms")
    public Result<List<TradePlatformDTO.Response>> getPlatforms(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        log.info("[getPlatforms] 获取平台列表: activeOnly={}", activeOnly);
        List<TradePlatformDTO.Response> platforms = stockNoteService.getPlatforms(activeOnly);
        return Result.success(platforms);
    }

    /**
     * 根据ID获取平台
     * GET /api/stock-notes/platforms/{id}
     *
     * @param id 平台ID
     * @return 平台信息
     */
    @GetMapping("/platforms/{id}")
    public Result<TradePlatformDTO.Response> getPlatformById(@PathVariable Long id) {
        log.info("[getPlatformById] 获取平台: id={}", id);
        return stockNoteService.getPlatformById(id)
                .map(Result::success)
                .orElse(Result.error(404, "平台不存在"));
    }

    /**
     * 更新平台信息
     * PUT /api/stock-notes/platforms/{id}
     *
     * @param id      平台ID
     * @param request 更新请求
     * @return 更新后的平台信息
     */
    @PutMapping("/platforms/{id}")
    public Result<TradePlatformDTO.Response> updatePlatform(
            @PathVariable Long id,
            @RequestBody TradePlatformDTO.UpdateRequest request) {
        log.info("[updatePlatform] 更新平台: id={}", id);
        return stockNoteService.updatePlatform(id, request)
                .map(Result::success)
                .orElse(Result.error(404, "平台不存在"));
    }

    /**
     * 删除平台
     * DELETE /api/stock-notes/platforms/{id}
     *
     * @param id 平台ID
     * @return 操作结果
     */
    @DeleteMapping("/platforms/{id}")
    public Result<Void> deletePlatform(@PathVariable Long id) {
        log.info("[deletePlatform] 删除平台: id={}", id);
        stockNoteService.deletePlatform(id);
        return Result.success(null);
    }

    // ==================== 持仓管理API ====================

    /**
     * 创建持仓记录
     * POST /api/stock-notes/positions
     *
     * @param request 创建请求
     * @return 持仓信息
     */
    @PostMapping("/positions")
    public Result<StockPositionDTO.Response> createPosition(
            @Valid @RequestBody StockPositionDTO.CreateRequest request) {
        log.info("[createPosition] 创建持仓: code={}, quantity={}", request.getCode(), request.getBuyQuantity());
        StockPositionDTO.Response response = stockNoteService.createPosition(request);
        return Result.success(response);
    }

    /**
     * 获取持仓列表
     * GET /api/stock-notes/positions
     *
     * @param platformId 平台ID(可选)
     * @param status     状态(可选)
     * @param code       股票代码(可选)
     * @return 持仓列表
     */
    @GetMapping("/positions")
    public Result<List<StockPositionDTO.Response>> getPositions(
            @RequestParam(required = false) Long platformId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code) {
        log.info("[getPositions] 获取持仓列表: platformId={}, status={}, code={}", platformId, status, code);
        List<StockPositionDTO.Response> positions = stockNoteService.getPositions(platformId, status, code);
        return Result.success(positions);
    }

    /**
     * 获取持仓中的股票列表
     * GET /api/stock-notes/positions/holding
     *
     * @param platformId 平台ID(可选)
     * @return 持仓列表
     */
    @GetMapping("/positions/holding")
    public Result<List<StockPositionDTO.Response>> getHoldingPositions(
            @RequestParam(required = false) Long platformId) {
        log.info("[getHoldingPositions] 获取持仓中的股票: platformId={}", platformId);
        List<StockPositionDTO.Response> positions = stockNoteService.getHoldingPositions(platformId);
        return Result.success(positions);
    }

    /**
     * 根据ID获取持仓
     * GET /api/stock-notes/positions/{id}
     *
     * @param id 持仓ID
     * @return 持仓信息
     */
    @GetMapping("/positions/{id}")
    public Result<StockPositionDTO.Response> getPositionById(@PathVariable Long id) {
        log.info("[getPositionById] 获取持仓: id={}", id);
        return stockNoteService.getPositionById(id)
                .map(Result::success)
                .orElse(Result.error(404, "持仓不存在"));
    }

    /**
     * 更新持仓信息
     * PUT /api/stock-notes/positions/{id}
     *
     * @param id      持仓ID
     * @param request 更新请求
     * @return 更新后的持仓信息
     */
    @PutMapping("/positions/{id}")
    public Result<StockPositionDTO.Response> updatePosition(
            @PathVariable Long id,
            @RequestBody StockPositionDTO.UpdateRequest request) {
        log.info("[updatePosition] 更新持仓: id={}", id);
        return stockNoteService.updatePosition(id, request)
                .map(Result::success)
                .orElse(Result.error(404, "持仓不存在"));
    }

    /**
     * 更新持仓当前价格和盈亏
     * PUT /api/stock-notes/positions/{id}/price
     *
     * @param id      持仓ID
     * @param request 更新价格请求
     * @return 更新后的持仓信息
     */
    @PutMapping("/positions/{id}/price")
    public Result<StockPositionDTO.Response> updatePositionPrice(
            @PathVariable Long id,
            @Valid @RequestBody StockPositionDTO.UpdatePriceRequest request) {
        log.info("[updatePositionPrice] 更新持仓价格: id={}, currentPrice={}", id, request.getCurrentPrice());
        return stockNoteService.updatePositionPrice(id, request)
                .map(Result::success)
                .orElse(Result.error(404, "持仓不存在或已清仓"));
    }

    /**
     * 卖出持仓
     * POST /api/stock-notes/positions/{id}/sell
     *
     * @param id      持仓ID
     * @param request 卖出请求
     * @return 更新后的持仓信息
     */
    @PostMapping("/positions/{id}/sell")
    public Result<StockPositionDTO.Response> sellPosition(
            @PathVariable Long id,
            @Valid @RequestBody StockPositionDTO.SellRequest request) {
        log.info("[sellPosition] 卖出持仓: id={}, sellPrice={}", id, request.getSellPrice());
        return stockNoteService.sellPosition(id, request)
                .map(Result::success)
                .orElse(Result.error(404, "持仓不存在"));
    }

    /**
     * 删除持仓
     * DELETE /api/stock-notes/positions/{id}
     *
     * @param id 持仓ID
     * @return 操作结果
     */
    @DeleteMapping("/positions/{id}")
    public Result<Void> deletePosition(@PathVariable Long id) {
        log.info("[deletePosition] 删除持仓: id={}", id);
        stockNoteService.deletePosition(id);
        return Result.success(null);
    }

    /**
     * 获取持仓汇总统计
     * GET /api/stock-notes/summary
     *
     * @param platformId 平台ID(可选)
     * @return 汇总统计
     */
    @GetMapping("/summary")
    public Result<StockPositionDTO.Summary> getSummary(
            @RequestParam(required = false) Long platformId) {
        log.info("[getSummary] 获取持仓汇总: platformId={}", platformId);
        StockPositionDTO.Summary summary = stockNoteService.getSummary(platformId);
        return Result.success(summary);
    }
}
