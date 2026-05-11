package com.mootdx.server.controller;

import com.mootdx.server.common.Result;
import com.mootdx.server.dto.F10ParseRequest;
import com.mootdx.server.dto.F10ParseResult;
import com.mootdx.server.service.F10ParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * F10数据解析控制器
 * 功能：提供F10数据解析API
 */
@Slf4j
@RestController
@RequestMapping("/api/f10")
@RequiredArgsConstructor
public class F10ParserController {

    private final F10ParserService f10ParserService;

    /**
     * 解析F10数据
     * @param request 解析请求
     * @return 解析结果
     */
    @PostMapping("/parse")
    public Result<F10ParseResult> parseF10Data(@RequestBody F10ParseRequest request) {
        log.info("[parseF10Data] 开始解析F10数据");
        
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return Result.error("内容不能为空");
        }
        
        try {
            F10ParseResult result = f10ParserService.parse(request.getContent());
            if (result.getSuccess()) {
                log.info("[parseF10Data] 解析成功，股票代码：{}，财务指标数量：{}，EPS历史数量：{}，风险提示数量：{}", 
                    result.getMetadata().getStockCode(),
                    result.getFinancialIndicators() != null ? result.getFinancialIndicators().size() : 0,
                    result.getEpsHistory() != null ? result.getEpsHistory().size() : 0,
                    result.getRiskAlerts() != null ? result.getRiskAlerts().size() : 0);
                return Result.success(result);
            } else {
                return Result.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("[parseF10Data] 解析失败", e);
            return Result.error("解析失败：" + e.getMessage());
        }
    }
}
