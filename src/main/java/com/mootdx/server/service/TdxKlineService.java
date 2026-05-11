package com.mootdx.server.service;

import com.mootdx.model.CompanyInfo;
import com.mootdx.server.config.TdxServerProperties;
import com.mootdx.server.dto.KlineDataDTO;
import com.mootdx.server.entity.TdxServerConfig;
import com.mootdx.quotes.TdxQuoteClient;
import com.mootdx.model.BarData;
import com.mootdx.model.StockMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * TDX K线数据服务
 * 负责从TDX服务器获取K线数据
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdxKlineService {

    private final TdxServerService serverService;
    private final TdxServerProperties tdxServerProperties;

    // TDX频率代码映射（根据TDX官方频率参数）
    private static final Map<String, Integer> FREQ_MAP = new HashMap<>();
    // 数据量配置
    private static final Map<String, Integer> FREQ_OFFSET_MAP = new HashMap<>();

    static {
        // 初始化频率映射（与pytdx保持一致）
        // 参考: pytdx get_security_bars 函数
        FREQ_MAP.put("1min", 8);      // 1分钟K线
        FREQ_MAP.put("5min", 0);      // 5分钟K线
        FREQ_MAP.put("15min", 1);     // 15分钟K线
        FREQ_MAP.put("30min", 2);     // 30分钟K线
        FREQ_MAP.put("60min", 3);     // 60分钟K线
        FREQ_MAP.put("day", 9);       // 日K线 (pytdx使用9，不是4)
        FREQ_MAP.put("week", 5);      // 周K线
        FREQ_MAP.put("month", 6);     // 月K线
        FREQ_MAP.put("quarter", 10);  // 季K线（3个月）
        FREQ_MAP.put("year", 11);     // 年K线

        // 初始化数据量配置
        FREQ_OFFSET_MAP.put("1min", 120);    // 2小时
        FREQ_OFFSET_MAP.put("5min", 120);    // 10小时
        FREQ_OFFSET_MAP.put("15min", 120);   // 30小时
        FREQ_OFFSET_MAP.put("30min", 120);   // 60小时
        FREQ_OFFSET_MAP.put("60min", 120);   // 120小时
        FREQ_OFFSET_MAP.put("day", 500);     // 约2年
        FREQ_OFFSET_MAP.put("week", 260);    // 约5年
        FREQ_OFFSET_MAP.put("month", 120);   // 约10年
        FREQ_OFFSET_MAP.put("quarter", 60);  // 约15年
        FREQ_OFFSET_MAP.put("year", 30);     // 约30年
    }

    // TDX客户端
    private TdxQuoteClient tdxClient;
    private String currentHost;
    private int currentPort;

    // 异步执行器
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    // K线请求超时时间（秒）
    private static final int KLINE_TIMEOUT_SECONDS = 10;

    /**
     * 初始化TDX客户端
     */
    @PostConstruct
    public void init() {
        connectToBestServer();
    }

    /**
     * 连接到最佳可用服务器
     * 策略：使用统一的create方法，由TdxQuoteClient内部处理固定IP和自动选择逻辑
     */
    private void connectToBestServer() {
        try {
            log.info("[connectToBestServer] 使用统一策略连接TDX服务器");
            
            // 使用统一的create方法，传入null让内部处理固定IP或自动选择
            tdxClient = TdxQuoteClient.create("180.153.18.170", 7709, tdxServerProperties.isHeartbeat());
            currentHost = tdxClient.getServerHost();
            currentPort = tdxClient.getServerPort();
            
            log.info("[connectToBestServer] 尝试连接服务器: {}:{}", currentHost, currentPort);
            tdxClient.connect();
            
            // 检查是否使用了固定IP
            if (tdxClient.isUsingFixedHost()) {
                log.info("[connectToBestServer] TDX客户端连接成功 (使用固定IP: {}:{})", currentHost, currentPort);
            } else {
                log.info("[connectToBestServer] TDX客户端连接成功 (自动选择服务器: {}:{})", currentHost, currentPort);
            }
        } catch (Exception e) {
            log.error("[connectToBestServer] TDX客户端连接失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 销毁TDX客户端
     */
    @PreDestroy
    public void destroy() {
        if (tdxClient != null) {
            try {
                log.info("[destroy] 关闭TDX客户端");
                tdxClient.disconnect();
            } catch (Exception e) {
                log.error("[destroy] 关闭TDX客户端失败: {}", e.getMessage());
            }
        }

        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            log.info("[destroy] 关闭线程池");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    /**
     * 确保客户端已连接
     */
    private void ensureConnected() {
        if (tdxClient == null) {
            // 尝试重新连接
            connectToBestServer();
            if (tdxClient == null) {
                throw new RuntimeException("TDX客户端未初始化，没有可用的服务器");
            }
        }
        // 重新连接如果需要
        try {
            tdxClient.connect();
        } catch (Exception e) {
            log.error("[ensureConnected] 重新连接失败: {}，尝试切换到其他服务器", e.getMessage());
            // 尝试连接到其他服务器
            connectToBestServer();
            if (tdxClient == null) {
                throw new RuntimeException("TDX服务器连接失败，没有可用的服务器", e);
            }
        }
    }

    /**
     * 从TDX服务器获取K线数据（带超时处理）
     *
     * @param code   股票代码
     * @param market 市场代码(SH/SZ)
     * @param freq   频率
     * @param limit  数量限制
     * @return K线数据列表
     */
    public List<KlineDataDTO> getKlineDataFromTdx(String code, String market, String freq, int limit) {
        log.info("[getKlineDataFromTdx] 从TDX获取K线数据: code={}, market={}, freq={}, limit={}", code, market, freq, limit);

        try {
            log.info("[getKlineDataFromTdx] 检查连接状态...");
            ensureConnected();
            log.info("[getKlineDataFromTdx] 连接状态正常");

            // 转换市场代码
            String symbol = code;
            log.info("[getKlineDataFromTdx] 股票代码: {}", symbol);

            // 获取频率代码
            int frequency = FREQ_MAP.getOrDefault(freq, 4);  // 默认日线
            int offset = Math.min(limit, FREQ_OFFSET_MAP.getOrDefault(freq, 500));
            log.info("[getKlineDataFromTdx] 频率代码: {}, 请求数量: {}", frequency, offset);

            // 使用异步调用并设置超时
            final int finalFrequency = frequency;
            final int finalOffset = offset;
            Future<List<BarData>> future = executorService.submit(() ->
                tdxClient.getKLine(symbol, finalFrequency, finalOffset)
            );

            log.info("[getKlineDataFromTdx] 开始调用tdxClient.getKLine（超时{}秒）...", KLINE_TIMEOUT_SECONDS);
            List<BarData> barDataList;
            try {
                barDataList = future.get(KLINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.error("[getKlineDataFromTdx] 获取K线数据超时（{}秒）", KLINE_TIMEOUT_SECONDS);
                future.cancel(true);
                return new ArrayList<>();
            }

            log.info("[getKlineDataFromTdx] tdxClient返回数据: {}条", barDataList != null ? barDataList.size() : "null");

            if (barDataList == null) {
                log.warn("[getKlineDataFromTdx] tdxClient返回null");
                return new ArrayList<>();
            }

            // 转换为DTO
            List<KlineDataDTO> result = convertBarDataToDTO(barDataList, freq);

            log.info("[getKlineDataFromTdx] 成功获取K线数据: {}条", result.size());
            return result;

        } catch (Exception e) {
            log.error("[getKlineDataFromTdx] 获取K线数据失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 将BarData转换为KlineDataDTO
     */
    private List<KlineDataDTO> convertBarDataToDTO(List<BarData> barDataList, String freq) {
        List<KlineDataDTO> result = new ArrayList<>();

        for (BarData bar : barDataList) {
            try {
                KlineDataDTO dto = new KlineDataDTO();

                // 解析日期
                long dateInt = bar.getDate();
                log.debug("[convertBarDataToDTO] 处理BarData, dateInt={}, freq={}", dateInt, freq);
                LocalDateTime dateTime = parseDateTime(dateInt, freq);
                log.debug("[convertBarDataToDTO] 解析日期成功: {}", dateTime);
                dto.setTradeDate(dateTime.toLocalDate());

                // 如果是分钟频率，设置时间
                if (isMinuteFreq(freq)) {
                    dto.setTradeTime(dateTime);
                }

                // 设置价格
                dto.setOpen(bar.getOpen());
                dto.setHigh(bar.getHigh());
                dto.setLow(bar.getLow());
                dto.setClose(bar.getClose());

                // 设置成交量和成交额
                dto.setVolume(bar.getVolume());
                dto.setAmount(bar.getAmount());

                result.add(dto);
            } catch (Exception e) {
                log.warn("[convertBarDataToDTO] 转换BarData失败: {}", e.getMessage());
            }
        }

        // 按时间倒序排序
        try {
            result.sort((a, b) -> {
                int dateCompare = b.getTradeDate().compareTo(a.getTradeDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                // 如果日期相同，比较时间
                if (a.getTradeTime() != null && b.getTradeTime() != null) {
                    return b.getTradeTime().compareTo(a.getTradeTime());
                }
                return 0;
            });
        } catch (Exception e) {
            log.error("[convertBarDataToDTO] 排序失败: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(long dateInt, String freq) {
        // 根据频率解析日期格式
        // 日K线: YYYYMMDD 格式 (8位)
        // 分钟K线: YYYYMMDDHHMM 格式 (12位)

        if (isMinuteFreq(freq)) {
            // 分钟K线: YYYYMMDDHHMM
            int year = (int) (dateInt / 100000000L);
            int month = (int) ((dateInt % 100000000L) / 1000000L);
            int day = (int) ((dateInt % 1000000L) / 10000L);
            int hour = (int) ((dateInt % 10000L) / 100L);
            int minute = (int) (dateInt % 100L);
            return LocalDateTime.of(year, month, day, hour, minute);
        } else {
            // 日K线: YYYYMMDD
            int year = (int) (dateInt / 10000L);
            int month = (int) ((dateInt % 10000L) / 100L);
            int day = (int) (dateInt % 100L);
            return LocalDateTime.of(year, month, day, 0, 0);
        }
    }

    /**
     * 判断是否为分钟频率
     */
    private boolean isMinuteFreq(String freq) {
        return freq != null && (freq.contains("min") || freq.equals("1m") || freq.equals("5m") ||
                freq.equals("15m") || freq.equals("30m") || freq.equals("60m"));
    }

    /**
     * 获取公司资料(F10)
     *
     * @param market 市场代码(SH/SZ)
     * @param code   股票代码
     * @return 公司资料
     */
    public CompanyInfo getCompanyInfo(String market, String code) {
        log.info("[getCompanyInfo] ==========================================");
        log.info("[getCompanyInfo] 获取公司资料: market={}, code={}", market, code);

        try {
            // 检查tdxClient状态
            log.info("[getCompanyInfo] tdxClient是否为null: {}", tdxClient == null);
            if (tdxClient != null) {
                log.info("[getCompanyInfo] tdxClient连接状态: {}", tdxClient.isConnected());
            }

            // 确保已连接
            if (tdxClient == null || !tdxClient.isConnected()) {
                log.warn("[getCompanyInfo] TDX客户端未连接，尝试重新连接");
                connectToBestServer();
            }

            // 检查连接是否成功
            if (tdxClient == null || !tdxClient.isConnected()) {
                log.error("[getCompanyInfo] TDX客户端连接失败，无法获取公司资料");
                return null;
            }

            log.info("[getCompanyInfo] TDX客户端连接正常，准备获取公司资料");

            // 转换市场代码
            int marketCode = "SH".equals(market) ? 1 : 0;
            log.info("[getCompanyInfo] 市场代码转换: {} -> {}", market, marketCode);

            // 获取公司信息
            log.info("[getCompanyInfo] 调用tdxClient.getCompanyInfo...");
            CompanyInfo result = null;
            try {
                result = tdxClient.getCompanyInfo(marketCode, code);
                log.info("[getCompanyInfo] tdxClient返回结果: {}", result == null ? "null" : result.getClass().getName());
            } catch (Exception ex) {
                log.error("[getCompanyInfo] tdxClient.getCompanyInfo抛出异常: {}", ex.getMessage());
                log.error("[getCompanyInfo] 异常类型: {}", ex.getClass().getName());
                if (ex.getCause() != null) {
                    log.error("[getCompanyInfo] 根本原因: {}", ex.getCause().getMessage());
                }
                throw ex;
            }

            return result;

        } catch (Exception e) {
            log.error("[getCompanyInfo] 获取公司资料失败: {}", e.getMessage(), e);
            return null;
        } finally {
            log.info("[getCompanyInfo] ==========================================");
        }
    }

    /**
     * 获取股票列表
     *
     * @param market 市场代码(0=深圳, 1=上海)
     * @return 股票列表
     */
    public List<StockMeta> getStockList(int market) {
        log.info("[getStockList] 获取股票列表: market={}", market);

        try {
            // 确保已连接
            if (tdxClient == null || !tdxClient.isConnected()) {
                log.warn("[getStockList] TDX客户端未连接，尝试重新连接");
                connectToBestServer();
            }

            // 检查连接是否成功
            if (tdxClient == null || !tdxClient.isConnected()) {
                log.error("[getStockList] TDX客户端连接失败，无法获取股票列表");
                return null;
            }

            // 获取股票列表
            return tdxClient.getStockList(market);

        } catch (Exception e) {
            log.error("[getStockList] 获取股票列表失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
