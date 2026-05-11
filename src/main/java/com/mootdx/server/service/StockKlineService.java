package com.mootdx.server.service;

import com.mootdx.model.CompanyInfo;
import com.mootdx.model.StockMeta;
import com.mootdx.server.dto.KlineDataDTO;
import com.mootdx.server.dto.KlineRequestDTO;
import com.mootdx.server.dto.KlineResponseDTO;
import com.mootdx.server.entity.StockKline;
import com.mootdx.server.repository.StockKlineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * K线数据服务
 * 负责K线数据的查询、缓存和管理
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@Service
public class StockKlineService {

    @Autowired
    private StockKlineRepository stockKlineRepository;

    @Autowired
    private TdxKlineService tdxKlineService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 获取K线数据
     * 优先从数据库查询，如不存在则从TDX获取并缓存
     *
     * @param request 请求参数
     * @return K线数据响应
     */
    public KlineResponseDTO getKlineData(KlineRequestDTO request) {
        log.info("[getKlineData] 获取K线数据: code={}, market={}, freq={}, limit={}",
                request.getCode(), request.getMarket(), request.getFreq(), request.getLimit());

        List<KlineDataDTO> data;

        // 判断是否强制刷新
        if (Boolean.TRUE.equals(request.getForceRefresh())) {
            log.info("[getKlineData] 强制刷新，从TDX获取数据");
            data = fetchFromTdxAndCache(request);
        } else {
            // 先尝试从数据库获取
//             data = fetchFromDatabase(request);

            // 如果数据库没有数据，从TDX获取
//            if (data.isEmpty()) {
                log.info("[getKlineData] 数据库无数据，从TDX获取");
                data = fetchFromTdxAndCache(request);
//            } else {
//                log.info("[getKlineData] 从数据库获取到{}条数据", data.size());
//            }
        }

        return KlineResponseDTO.builder()
                .code(request.getCode())
                .market(request.getMarket())
                .freq(request.getFreq())
                .data(data)
                .build();
    }

    /**
     * 从数据库获取K线数据
     */
    private List<KlineDataDTO> fetchFromDatabase(KlineRequestDTO request) {
        List<StockKline> entities;

        // 如果有日期范围，按日期范围查询
        if (request.getStart() != null && request.getEnd() != null) {
            LocalDate startDate = LocalDate.parse(request.getStart(), DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(request.getEnd(), DATE_FORMATTER);
            entities = stockKlineRepository.findByCodeAndMarketAndFreqAndTradeDateBetweenOrderByTradeDateDesc(
                    request.getCode(), request.getMarket(), request.getFreq(), startDate, endDate);
        } else {
            // 否则按数量限制查询
            entities = stockKlineRepository.findTop500ByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(
                    request.getCode(), request.getMarket(), request.getFreq());
        }

        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 从TDX获取数据并缓存到数据库
     */
    @Transactional
    private List<KlineDataDTO> fetchFromTdxAndCache(KlineRequestDTO request) {
        // 从TDX获取数据
        List<KlineDataDTO> data = tdxKlineService.getKlineDataFromTdx(
                request.getCode(), request.getMarket(), request.getFreq(), request.getLimit());

        if (data.isEmpty()) {
            log.warn("[fetchFromTdxAndCache] 从TDX获取数据为空");
            return data;
        }

        log.info("[fetchFromTdxAndCache] 从TDX获取到{}条数据，开始缓存", data.size());

        // 保存到数据库
        for (KlineDataDTO dto : data) {
            try {
                // 检查是否已存在
                boolean exists = stockKlineRepository.existsByCodeAndMarketAndFreqAndTradeDate(
                        request.getCode(), request.getMarket(), request.getFreq(), dto.getTradeDate());

                if (!exists) {
                    StockKline entity = convertToEntity(dto, request.getCode(), request.getMarket(), request.getFreq());
                    stockKlineRepository.save(entity);
                }
            } catch (Exception e) {
                log.warn("[fetchFromTdxAndCache] 保存K线数据失败: {}", e.getMessage());
            }
        }

        log.info("[fetchFromTdxAndCache] 数据缓存完成");
        return data;
    }

    /**
     * 将实体转换为DTO
     */
    private KlineDataDTO convertToDTO(StockKline entity) {
        return KlineDataDTO.builder()
                .tradeDate(entity.getTradeDate())
                .tradeTime(entity.getTradeTime())
                .open(entity.getOpen())
                .high(entity.getHigh())
                .low(entity.getLow())
                .close(entity.getClose())
                .volume(entity.getVolume())
                .amount(entity.getAmount())
                .build();
    }

    /**
     * 将DTO转换为实体
     */
    private StockKline convertToEntity(KlineDataDTO dto, String code, String market, String freq) {
        return StockKline.builder()
                .code(code)
                .market(market)
                .freq(freq)
                .tradeDate(dto.getTradeDate())
                .tradeTime(dto.getTradeTime())
                .open(dto.getOpen())
                .high(dto.getHigh())
                .low(dto.getLow())
                .close(dto.getClose())
                .volume(dto.getVolume())
                .amount(dto.getAmount())
                .build();
    }

    /**
     * 清除指定股票、频率的缓存数据
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     */
    @Transactional
    public void clearCache(String code, String market, String freq) {
        log.info("[clearCache] 清除K线缓存: code={}, market={}, freq={}", code, market, freq);
        stockKlineRepository.deleteByCodeAndMarketAndFreq(code, market, freq);
    }

    /**
     * 获取最新的K线数据
     *
     * @param code   股票代码
     * @param market 市场代码
     * @param freq   频率
     * @return 最新的K线数据
     */
    public KlineDataDTO getLatestKline(String code, String market, String freq) {
        return stockKlineRepository.findTopByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(code, market, freq)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * 获取公司资料(F10)
     *
     * @param market 市场代码(SH/SZ)
     * @param code   股票代码
     * @return 公司资料
     */
    public CompanyInfo getCompanyInfo(String market, String code) {
        log.info("[getCompanyInfo] 获取公司资料: market={}, code={}", market, code);
        return tdxKlineService.getCompanyInfo(market, code);
    }

    /**
     * 获取股票列表
     *
     * @param market 市场代码(0=深圳, 1=上海)
     * @return 股票列表
     */
    public List<StockMeta> getStockList(int market) {
        log.info("[getStockList] 获取股票列表: market={}", market);
        return tdxKlineService.getStockList(market);
    }
}
