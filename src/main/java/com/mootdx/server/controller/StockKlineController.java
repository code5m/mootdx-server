package com.mootdx.server.controller;

import com.mootdx.model.CompanyInfo;
import com.mootdx.model.StockMeta;
import com.mootdx.server.common.Result;
import com.mootdx.server.dto.CompanyInfoDTO;
import com.mootdx.server.dto.KlineRequestDTO;
import com.mootdx.server.dto.KlineResponseDTO;
import com.mootdx.server.service.CompanyInfoParserService;
import com.mootdx.server.service.StockKlineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * K线数据Controller
 * 提供K线数据查询API接口
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@Validated
@CrossOrigin(origins = "*")
public class StockKlineController {

    @Autowired
    private StockKlineService stockKlineService;

    @Autowired
    private CompanyInfoParserService companyInfoParserService;

    /**
     * 获取K线数据
     * GET /api/stocks/kline/{market}/{code}
     *
     * @param market  市场代码(SH/SZ)
     * @param code    股票代码
     * @param request 请求参数（包含freq、start、end、limit等）
     * @return K线数据响应
     */
    @GetMapping("/kline/{market}/{code}")
    public Result<KlineResponseDTO> getKlineData(
            @PathVariable String market,
            @PathVariable String code,
            @Valid KlineRequestDTO request) {

        log.info("[getKlineData] 收到K线数据请求: market={}, code={}", market, code);

        // 设置路径参数
        request.setMarket(market.toUpperCase());
        request.setCode(code);

        // 获取K线数据
        KlineResponseDTO response = stockKlineService.getKlineData(request);

        log.info("[getKlineData] 返回K线数据: {}条", response.getData().size());
        return Result.success(response);
    }

    /**
     * 清除K线缓存
     * DELETE /api/stocks/kline/{market}/{code}/cache
     *
     * @param market 市场代码(SH/SZ)
     * @param code   股票代码
     * @param freq   频率
     * @return 操作结果
     */
    @DeleteMapping("/kline/{market}/{code}/cache")
    public Result<Void> clearKlineCache(
            @PathVariable String market,
            @PathVariable String code,
            @RequestParam String freq) {

        log.info("[clearKlineCache] 清除K线缓存: market={}, code={}, freq={}", market, code, freq);

        stockKlineService.clearCache(code, market.toUpperCase(), freq);

        return Result.success(null);
    }

    /**
     * 获取公司资料(F10)
     * GET /api/stocks/company/{market}/{code}
     *
     * @param market 市场代码(SH/SZ)
     * @param code   股票代码
     * @return 公司资料
     */
    @GetMapping("/company/{market}/{code}")
    public Result<CompanyInfoDTO> getCompanyInfo(
            @PathVariable String market,
            @PathVariable String code) {

        log.info("[getCompanyInfo] ==========================================");
        log.info("[getCompanyInfo] 收到请求: market={}, code={}", market, code);
        log.info("[getCompanyInfo] 请求路径: /api/stocks/company/{}/{}", market, code);

        try {
            String upperMarket = market.toUpperCase();
            log.info("[getCompanyInfo] 转换市场代码: {} -> {}", market, upperMarket);

            log.info("[getCompanyInfo] 调用stockKlineService.getCompanyInfo...");
            CompanyInfo companyInfo = stockKlineService.getCompanyInfo(upperMarket, code);
            log.info("[getCompanyInfo] stockKlineService返回结果: {}", companyInfo == null ? "null" : companyInfo.getClass().getName());

            if (companyInfo == null) {
                log.warn("[getCompanyInfo] 公司资料为空，返回错误");
                return Result.error(500, "获取公司资料失败，请检查TDX服务器连接");
            }

            // 使用解析服务将原始数据转换为结构化DTO
            log.info("[getCompanyInfo] 开始解析公司资料数据...");
            CompanyInfoDTO companyInfoDTO = companyInfoParserService.parseCompanyInfo(companyInfo);
            log.info("[getCompanyInfo] 解析完成: code={}, name={}", companyInfoDTO.getCode(), companyInfoDTO.getName());

            log.info("[getCompanyInfo] 返回成功结果");
            return Result.success(companyInfoDTO);
        } catch (Exception e) {
            log.error("[getCompanyInfo] 获取公司资料异常: {}", e.getMessage(), e);
            return Result.error(500, "获取公司资料失败: " + e.getMessage());
        } finally {
            log.info("[getCompanyInfo] ==========================================");
        }
    }

    /**
     * 获取股票列表
     * GET /api/stocks/list/{market}
     *
     * @param market 市场代码(0=深圳, 1=上海)
     * @return 股票列表
     */
    @GetMapping("/list/{market}")
    public Result<List<StockMeta>> getStockList(@PathVariable("market") int market) {
        log.info("[getStockList] 获取股票列表: market={}", market);

        List<StockMeta> stockList = stockKlineService.getStockList(market);

        if (stockList == null) {
            log.error("[getStockList] 获取股票列表失败，返回空列表");
            return Result.error(500, "获取股票列表失败，请检查TDX服务器连接");
        }

        return Result.success(stockList);
    }
}
