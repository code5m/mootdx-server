package com.mootdx.server.service;

import com.mootdx.localdata.TdxBlockConfigReader;
import com.mootdx.localdata.TdxBlockDataReader;
import com.mootdx.localdata.TdxLocalDataReader;
import com.mootdx.localdata.TdxMarkDataReader;
import com.mootdx.model.BarData;
import com.mootdx.server.entity.StockBlock;
import com.mootdx.server.entity.StockKline;
import com.mootdx.server.entity.StockMark;
import com.mootdx.server.repository.StockBlockRepository;
import com.mootdx.server.repository.StockKlineRepository;
import com.mootdx.server.repository.StockMarkRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 通达信数据导入服务
 * 支持全量导入、增量导入和差异导入
 * 优化版本：使用批量插入和多线程并行处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdxDataImportService {

    private final StockKlineRepository stockKlineRepository;
    private final StockBlockRepository stockBlockRepository;
    private final StockMarkRepository stockMarkRepository;
    private final PlatformTransactionManager transactionManager;
    private final TdxLocalDataReader localDataReader = new TdxLocalDataReader();
    private final TdxBlockDataReader blockDataReader = new TdxBlockDataReader();
    private final TdxBlockConfigReader blockConfigReader = new TdxBlockConfigReader();
    private final TdxMarkDataReader markDataReader = new TdxMarkDataReader();

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private static final String TDX_INSTALL_DIR = "C:\\new_tdx";

    // 批处理大小配置
    private static final int BATCH_SIZE = 2000;  // 增大到2000条
    private static final int FILE_BATCH_SIZE = 20; // 每批处理20个文件
    private static final int THREAD_POOL_SIZE = 4; // 线程池大小

    // 线程池用于并行处理文件
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * 导入类型枚举
     */
    public enum ImportType {
        FULL,       // 全量导入：删除旧数据，重新导入
        INCREMENTAL, // 增量导入：只导入数据库中不存在的数据
        DIFFERENTIAL // 差异导入：对比并更新有差异的数据
    }

    /**
     * 导入结果
     */
    @Data
    @Builder
    public static class ImportResult {
        private int successCount;      // 成功导入文件数
        private int failCount;         // 失败文件数
        private long totalRecords;     // 总记录数
        private long insertedCount;    // 新增记录数
        private long updatedCount;     // 更新记录数
        private long skippedCount;     // 跳过记录数（已存在且无变化）
        private String message;        // 结果消息
    }

    /**
     * 数据差异信息
     */
    @Data
    @Builder
    public static class DataDiff {
        private String code;           // 股票代码
        private String market;         // 市场
        private LocalDate tradeDate;   // 交易日期
        private String diffType;       // 差异类型：NEW(新增), MODIFIED(修改), SAME(相同)
        private BarData fileData;      // 文件中的数据
        private StockKline dbData;     // 数据库中的数据
    }

    /**
     * 导入日线数据 - 高性能版本
     * 使用批量插入和多线程并行处理
     *
     * @param market     市场(sh/sz/bj)
     * @param fileNames  文件名列表
     * @param importType 导入类型
     * @return 导入结果
     */
    public ImportResult importDayData(String market, List<String> fileNames, ImportType importType) {
        log.info("导入日线数据(高性能模式): market={}, files={}, type={}", market, fileNames.size(), importType);

        long startTime = System.currentTimeMillis();
        AtomicLong totalRecords = new AtomicLong(0);
        AtomicLong insertedCount = new AtomicLong(0);
        AtomicLong updatedCount = new AtomicLong(0);
        AtomicLong skippedCount = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        // 使用线程池并行处理文件，每个文件独立事务
        List<Future<?>> futures = new ArrayList<>();

        for (String fileName : fileNames) {
            Future<?> future = executorService.submit(() -> {
                try {
                    String filePath = TDX_INSTALL_DIR + "\\vipdoc\\" + market + "\\lday\\" + fileName;
                    log.info("开始读取文件: {}", filePath);
                    List<BarData> bars = localDataReader.readDayFile(filePath);
                    String stockCode = parseStockCodeFromFileName(fileName);
                    log.info("读取文件完成: {}, 股票代码: {}, 记录数: {}", fileName, stockCode, bars.size());

                    // 处理单个文件的数据（在独立事务中）
                    FileResult result = transactionTemplate.execute(status -> {
                        try {
                            return processDayDataFast(stockCode, market, "day", bars, importType);
                        } catch (Exception ex) {
                            log.error("事务执行失败: {} - {}", fileName, ex.getMessage(), ex);
                            throw ex;
                        }
                    });

                    log.info("处理完成: {}, 总记录: {}, 新增: {}, 更新: {}, 跳过: {}",
                        fileName, result.totalRecords, result.inserted, result.updated, result.skipped);

                    totalRecords.addAndGet(result.totalRecords);
                    insertedCount.addAndGet(result.inserted);
                    updatedCount.addAndGet(result.updated);
                    skippedCount.addAndGet(result.skipped);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("导入失败: {} - {}", fileName, e.getMessage(), e);
                    failCount.incrementAndGet();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("等待任务完成时出错: {}", e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("导入完成，耗时: {}ms, 平均: {}条/秒", duration,
            duration > 0 ? (totalRecords.get() * 1000 / duration) : 0);

        return ImportResult.builder()
            .successCount(successCount.intValue())
            .failCount(failCount.intValue())
            .totalRecords(totalRecords.get())
            .insertedCount(insertedCount.get())
            .updatedCount(updatedCount.get())
            .skippedCount(skippedCount.get())
            .message(String.format("成功处理 %d 个文件，失败 %d 个。总计: %d 条记录，新增 %d 条，更新 %d 条，跳过 %d 条。耗时: %.2f秒",
                successCount.get(), failCount.get(), totalRecords.get(), insertedCount.get(),
                updatedCount.get(), skippedCount.get(), duration / 1000.0))
            .build();
    }

    /**
     * 在独立事务中处理日线数据
     */
    @Transactional
    public FileResult processDayDataInTransaction(String code, String market, String freq,
                                                   List<BarData> bars, ImportType importType) {
        return processDayDataFast(code, market, freq, bars, importType);
    }

    /**
     * 文件处理结果内部类
     */
    private static class FileResult {
        long totalRecords;
        long inserted;
        long updated;
        long skipped;
    }

    /**
     * 快速处理日线数据 - 使用批量插入
     */
    @Transactional
    public FileResult processDayDataFast(String code, String market, String freq,
                                          List<BarData> bars, ImportType importType) {
        FileResult result = new FileResult();
        result.totalRecords = bars.size();

        // 全量导入：先删除旧数据
        if (importType == ImportType.FULL) {
            stockKlineRepository.deleteByCodeAndMarketAndFreq(code, market, freq);
        }

        // 查询已存在的数据
        Map<LocalDate, StockKline> existingMap = new HashMap<>();
        if (importType == ImportType.DIFFERENTIAL || importType == ImportType.INCREMENTAL) {
            List<StockKline> existingList = stockKlineRepository
                .findByCodeAndMarketAndFreq(code, market, freq);
            existingMap = existingList.stream()
                .collect(Collectors.toMap(StockKline::getTradeDate, k -> k));
        }

        List<StockKline> toInsert = new ArrayList<>(BATCH_SIZE);
        List<StockKline> toUpdate = new ArrayList<>(BATCH_SIZE);

        for (BarData bar : bars) {
            LocalDate tradeDate = bar.getLocalDate();
            StockKline existing = existingMap.get(tradeDate);

            if (existing == null) {
                StockKline kline = convertToKline(code, market, freq, bar);
                toInsert.add(kline);
                result.inserted++;
            } else if (importType == ImportType.DIFFERENTIAL && isDataDifferent(existing, bar)) {
                StockKline kline = convertToKline(code, market, freq, bar);
                kline.setId(existing.getId());
                toUpdate.add(kline);
                result.updated++;
            } else {
                result.skipped++;
            }

            // 批量保存
            if (toInsert.size() >= BATCH_SIZE) {
                stockKlineRepository.saveAll(toInsert);
                toInsert.clear();
            }
            if (toUpdate.size() >= BATCH_SIZE) {
                stockKlineRepository.saveAll(toUpdate);
                toUpdate.clear();
            }
        }

        // 保存剩余数据
        if (!toInsert.isEmpty()) {
            stockKlineRepository.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            stockKlineRepository.saveAll(toUpdate);
        }

        return result;
    }

    /**
     * 处理日线数据导入逻辑 - 分批保存防止内存溢出
     */
    private ImportResult processDayData(String code, String market, String freq,
                                         List<BarData> bars, ImportType importType) {
        long inserted = 0;
        long updated = 0;
        long skipped = 0;

        // 全量导入：先删除旧数据
        if (importType == ImportType.FULL) {
            stockKlineRepository.deleteByCodeAndMarketAndFreq(code, market, freq);
        }

        // 差异导入时，批量查询已存在的数据
        Map<LocalDate, StockKline> existingMap = new HashMap<>();
        if (importType == ImportType.DIFFERENTIAL || importType == ImportType.INCREMENTAL) {
            List<StockKline> existingList = stockKlineRepository
                .findByCodeAndMarketAndFreq(code, market, freq);
            existingMap = existingList.stream()
                .collect(Collectors.toMap(StockKline::getTradeDate, k -> k));
        }

        // 分批处理，每批500条
        int batchSize = 500;
        List<StockKline> toInsert = new ArrayList<>();
        List<StockKline> toUpdate = new ArrayList<>();

        for (int i = 0; i < bars.size(); i++) {
            BarData bar = bars.get(i);
            LocalDate tradeDate = bar.getLocalDate();
            StockKline existing = existingMap.get(tradeDate);

            if (existing == null) {
                // 新增数据
                StockKline kline = convertToKline(code, market, freq, bar);
                toInsert.add(kline);
                inserted++;
            } else if (importType == ImportType.DIFFERENTIAL) {
                // 差异导入：检查数据是否有变化
                if (isDataDifferent(existing, bar)) {
                    // 更新数据
                    StockKline kline = convertToKline(code, market, freq, bar);
                    kline.setId(existing.getId());
                    toUpdate.add(kline);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                // 增量导入：已存在则跳过
                skipped++;
            }

            // 每批达到batchSize时保存一次
            if (toInsert.size() >= batchSize) {
                stockKlineRepository.saveAll(toInsert);
                toInsert.clear();
            }
            if (toUpdate.size() >= batchSize) {
                stockKlineRepository.saveAll(toUpdate);
                toUpdate.clear();
            }
        }

        // 保存剩余的数据
        if (!toInsert.isEmpty()) {
            stockKlineRepository.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            stockKlineRepository.saveAll(toUpdate);
        }

        return ImportResult.builder()
            .totalRecords(bars.size())
            .insertedCount(inserted)
            .updatedCount(updated)
            .skippedCount(skipped)
            .build();
    }

    /**
     * 导入分钟线数据 - 高性能版本
     * 使用批量插入和多线程并行处理
     */
    public ImportResult importMinuteData(String market, String minuteType,
                                          List<String> fileNames, ImportType importType) {
        log.info("导入分钟线数据(高性能模式): market={}, type={}, files={}, importType={}",
            market, minuteType, fileNames.size(), importType);

        String freq = "1min".equals(minuteType) ? "1min" : "5min";
        String subDir = "1min".equals(minuteType) ? "minline" : "fzline";

        long startTime = System.currentTimeMillis();
        AtomicLong totalRecords = new AtomicLong(0);
        AtomicLong insertedCount = new AtomicLong(0);
        AtomicLong updatedCount = new AtomicLong(0);
        AtomicLong skippedCount = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        // 使用线程池并行处理文件，每个文件独立事务
        List<Future<?>> futures = new ArrayList<>();

        for (String fileName : fileNames) {
            Future<?> future = executorService.submit(() -> {
                try {
                    String filePath = TDX_INSTALL_DIR + "\\vipdoc\\" + market + "\\" + subDir + "\\" + fileName;
                    List<BarData> bars = "1min".equals(minuteType)
                        ? localDataReader.readMinute1File(filePath)
                        : localDataReader.readMinute5File(filePath);

                    String stockCode = parseStockCodeFromFileName(fileName);

                    // 处理单个文件的数据（在独立事务中）
                    FileResult result = transactionTemplate.execute(status -> {
                        return processMinuteDataFast(stockCode, market, freq, bars, importType);
                    });

                    totalRecords.addAndGet(result.totalRecords);
                    insertedCount.addAndGet(result.inserted);
                    updatedCount.addAndGet(result.updated);
                    skippedCount.addAndGet(result.skipped);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("导入失败: {} - {}", fileName, e.getMessage(), e);
                    failCount.incrementAndGet();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("等待任务完成时出错: {}", e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("导入完成，耗时: {}ms, 平均: {}条/秒", duration,
            duration > 0 ? (totalRecords.get() * 1000 / duration) : 0);

        return ImportResult.builder()
            .successCount(successCount.intValue())
            .failCount(failCount.intValue())
            .totalRecords(totalRecords.get())
            .insertedCount(insertedCount.get())
            .updatedCount(updatedCount.get())
            .skippedCount(skippedCount.get())
            .message(String.format("成功处理 %d 个文件，失败 %d 个。总计: %d 条记录，新增 %d 条，更新 %d 条，跳过 %d 条。耗时: %.2f秒",
                successCount.get(), failCount.get(), totalRecords.get(), insertedCount.get(),
                updatedCount.get(), skippedCount.get(), duration / 1000.0))
            .build();
    }

    /**
     * 快速处理分钟线数据 - 使用批量插入
     */
    @Transactional
    public FileResult processMinuteDataFast(String code, String market, String freq,
                                             List<BarData> bars, ImportType importType) {
        FileResult result = new FileResult();
        result.totalRecords = bars.size();

        // 全量导入：先删除旧数据
        if (importType == ImportType.FULL) {
            stockKlineRepository.deleteByCodeAndMarketAndFreq(code, market, freq);
        }

        // 查询已存在的数据
        Map<LocalDateTime, StockKline> existingMap = new HashMap<>();
        if (importType == ImportType.DIFFERENTIAL || importType == ImportType.INCREMENTAL) {
            List<StockKline> existingList = stockKlineRepository
                .findByCodeAndMarketAndFreq(code, market, freq);
            existingMap = existingList.stream()
                .filter(k -> k.getTradeTime() != null)
                .collect(Collectors.toMap(
                    k -> k.getTradeTime(),
                    k -> k
                ));
        }

        List<StockKline> toInsert = new ArrayList<>(BATCH_SIZE);
        List<StockKline> toUpdate = new ArrayList<>(BATCH_SIZE);

        for (BarData bar : bars) {
            LocalDate tradeDate = bar.getLocalDate();
            long dateTime = bar.getDate();
            int hour = (int) ((dateTime % 10000) / 100);
            int minute = (int) (dateTime % 100);
            LocalDateTime tradeTime = tradeDate.atTime(hour, minute);

            StockKline existing = existingMap.get(tradeTime);

            if (existing == null) {
                StockKline kline = convertToMinuteKline(code, market, freq, bar, tradeTime);
                toInsert.add(kline);
                result.inserted++;
            } else if (importType == ImportType.DIFFERENTIAL && isDataDifferent(existing, bar)) {
                StockKline kline = convertToMinuteKline(code, market, freq, bar, tradeTime);
                kline.setId(existing.getId());
                toUpdate.add(kline);
                result.updated++;
            } else {
                result.skipped++;
            }

            // 批量保存
            if (toInsert.size() >= BATCH_SIZE) {
                stockKlineRepository.saveAll(toInsert);
                toInsert.clear();
            }
            if (toUpdate.size() >= BATCH_SIZE) {
                stockKlineRepository.saveAll(toUpdate);
                toUpdate.clear();
            }
        }

        // 保存剩余数据
        if (!toInsert.isEmpty()) {
            stockKlineRepository.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            stockKlineRepository.saveAll(toUpdate);
        }

        return result;
    }

    /**
     * 在独立事务中处理分钟线数据
     */
    @Transactional
    public FileResult processMinuteDataInTransaction(String code, String market, String freq,
                                                      List<BarData> bars, ImportType importType) {
        return processMinuteDataFast(code, market, freq, bars, importType);
    }

    /**
     * 处理分钟线数据导入逻辑 - 分批保存防止内存溢出
     */
    private ImportResult processMinuteData(String code, String market, String freq,
                                            List<BarData> bars, ImportType importType) {
        long inserted = 0;
        long updated = 0;
        long skipped = 0;

        // 全量导入：先删除旧数据
        if (importType == ImportType.FULL) {
            stockKlineRepository.deleteByCodeAndMarketAndFreq(code, market, freq);
        }

        // 批量查询已存在的数据
        Map<LocalDateTime, StockKline> existingMap = new HashMap<>();
        if (importType == ImportType.DIFFERENTIAL || importType == ImportType.INCREMENTAL) {
            List<StockKline> existingList = stockKlineRepository
                .findByCodeAndMarketAndFreq(code, market, freq);
            existingMap = existingList.stream()
                .filter(k -> k.getTradeTime() != null)
                .collect(Collectors.toMap(
                    k -> k.getTradeTime(),
                    k -> k
                ));
        }

        // 分批处理，每批500条
        int batchSize = 500;
        List<StockKline> toInsert = new ArrayList<>();
        List<StockKline> toUpdate = new ArrayList<>();

        for (int i = 0; i < bars.size(); i++) {
            BarData bar = bars.get(i);
            LocalDate tradeDate = bar.getLocalDate();
            // 分钟线时间从date中提取 (YYYYMMDDHHMM格式)
            long dateTime = bar.getDate();
            int hour = (int) ((dateTime % 10000) / 100);
            int minute = (int) (dateTime % 100);
            LocalDateTime tradeTime = tradeDate.atTime(hour, minute);

            StockKline existing = existingMap.get(tradeTime);

            if (existing == null) {
                StockKline kline = convertToMinuteKline(code, market, freq, bar, tradeTime);
                toInsert.add(kline);
                inserted++;
            } else if (importType == ImportType.DIFFERENTIAL) {
                if (isDataDifferent(existing, bar)) {
                    StockKline kline = convertToMinuteKline(code, market, freq, bar, tradeTime);
                    kline.setId(existing.getId());
                    toUpdate.add(kline);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                skipped++;
            }

            // 每批达到batchSize时保存一次
            if (toInsert.size() >= batchSize) {
                stockKlineRepository.saveAll(toInsert);
                toInsert.clear();
            }
            if (toUpdate.size() >= batchSize) {
                stockKlineRepository.saveAll(toUpdate);
                toUpdate.clear();
            }
        }

        // 保存剩余的数据
        if (!toInsert.isEmpty()) {
            stockKlineRepository.saveAll(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            stockKlineRepository.saveAll(toUpdate);
        }

        return ImportResult.builder()
            .totalRecords(bars.size())
            .insertedCount(inserted)
            .updatedCount(updated)
            .skippedCount(skipped)
            .build();
    }

    /**
     * 比对文件数据和数据库数据，返回差异信息
     */
    public List<DataDiff> compareDayData(String market, String fileName) throws IOException {
        String filePath = TDX_INSTALL_DIR + "\\vipdoc\\" + market + "\\lday\\" + fileName;
        List<BarData> bars = localDataReader.readDayFile(filePath);
        String stockCode = parseStockCodeFromFileName(fileName);

        List<DataDiff> diffs = new ArrayList<>();

        // 获取数据库中该股票的所有数据
        List<StockKline> dbData = stockKlineRepository
            .findTop500ByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(stockCode, market, "day");

        Map<LocalDate, StockKline> dbMap = dbData.stream()
            .collect(Collectors.toMap(StockKline::getTradeDate, k -> k));

        for (BarData bar : bars) {
            LocalDate tradeDate = bar.getLocalDate();
            StockKline dbRecord = dbMap.get(tradeDate);

            DataDiff.DataDiffBuilder diffBuilder = DataDiff.builder()
                .code(stockCode)
                .market(market)
                .tradeDate(tradeDate)
                .fileData(bar);

            if (dbRecord == null) {
                diffBuilder.diffType("NEW");
            } else if (isDataDifferent(dbRecord, bar)) {
                diffBuilder.diffType("MODIFIED")
                    .dbData(dbRecord);
            } else {
                diffBuilder.diffType("SAME")
                    .dbData(dbRecord);
            }

            diffs.add(diffBuilder.build());
        }

        return diffs;
    }

    /**
     * 比对分钟线数据差异
     */
    public List<DataDiff> compareMinuteData(String market, String minuteType, String fileName) throws IOException {
        String freq = "1min".equals(minuteType) ? "1min" : "5min";
        String subDir = "1min".equals(minuteType) ? "minline" : "fzline";
        String filePath = TDX_INSTALL_DIR + "\\vipdoc\\" + market + "\\" + subDir + "\\" + fileName;

        TdxLocalDataReader reader = new TdxLocalDataReader();
        List<BarData> bars = "1min".equals(minuteType)
            ? reader.readMinute1File(filePath)
            : reader.readMinute5File(filePath);

        String stockCode = parseStockCodeFromFileName(fileName);
        List<DataDiff> diffs = new ArrayList<>();

        // 获取数据库中该股票的所有数据
        List<StockKline> dbData = stockKlineRepository.findByCodeAndMarketAndFreq(stockCode, market, freq);

        Map<LocalDateTime, StockKline> dbMap = dbData.stream()
            .filter(k -> k.getTradeTime() != null)
            .collect(Collectors.toMap(
                k -> k.getTradeTime(),
                k -> k
            ));

        for (BarData bar : bars) {
            LocalDate tradeDate = bar.getLocalDate();
            long dateTime = bar.getDate();
            int hour = (int) ((dateTime % 10000) / 100);
            int minute = (int) (dateTime % 100);
            LocalDateTime tradeTime = tradeDate.atTime(hour, minute);

            StockKline dbRecord = dbMap.get(tradeTime);

            DataDiff.DataDiffBuilder diffBuilder = DataDiff.builder()
                .code(stockCode)
                .market(market)
                .tradeDate(tradeDate)
                .fileData(bar);

            if (dbRecord == null) {
                diffBuilder.diffType("NEW");
            } else if (isDataDifferent(dbRecord, bar)) {
                diffBuilder.diffType("MODIFIED")
                    .dbData(dbRecord);
            } else {
                diffBuilder.diffType("SAME")
                    .dbData(dbRecord);
            }

            diffs.add(diffBuilder.build());
        }

        return diffs;
    }

    /**
     * 获取数据库最新日期，用于增量导入
     */
    public LocalDate getLatestTradeDate(String code, String market, String freq) {
        Optional<StockKline> latest = stockKlineRepository
            .findTopByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(code, market, freq);
        return latest.map(StockKline::getTradeDate).orElse(null);
    }

    // ============ 私有辅助方法 ============

    private String parseStockCodeFromFileName(String fileName) {
        // 文件名格式: sh600000.day 或 600000.day
        String name = fileName.toLowerCase().replace(".day", "").replace(".lc1", "").replace(".lc5", "");
        if (name.startsWith("sh")) {
            return name.substring(2);
        } else if (name.startsWith("sz")) {
            return name.substring(2);
        }
        return name;
    }

    private Optional<StockKline> findExistingKline(String code, String market, String freq, LocalDate tradeDate) {
        // 由于可能存在多条记录，这里简化处理
        List<StockKline> list = stockKlineRepository
            .findTop500ByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(code, market, freq);
        return list.stream()
            .filter(k -> k.getTradeDate().equals(tradeDate))
            .findFirst();
    }

    private Optional<StockKline> findExistingMinuteKline(String code, String market, String freq,
                                                          LocalDate tradeDate, LocalDateTime tradeTime) {
        List<StockKline> list = stockKlineRepository
            .findTop500ByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(code, market, freq);
        return list.stream()
            .filter(k -> k.getTradeDate().equals(tradeDate))
            .filter(k -> k.getTradeTime() != null && k.getTradeTime().equals(tradeTime))
            .findFirst();
    }

    private boolean isDataDifferent(StockKline dbData, BarData fileData) {
        // 比较关键字段是否有差异 (BarData的价格已经是BigDecimal)
        return dbData.getOpen().compareTo(fileData.getOpen()) != 0 ||
               dbData.getHigh().compareTo(fileData.getHigh()) != 0 ||
               dbData.getLow().compareTo(fileData.getLow()) != 0 ||
               dbData.getClose().compareTo(fileData.getClose()) != 0 ||
               dbData.getVolume() != fileData.getVolume() ||
               dbData.getAmount().compareTo(fileData.getAmount()) != 0;
    }

    private StockKline convertToKline(String code, String market, String freq, BarData bar) {
        return StockKline.builder()
            .code(code)
            .market(market.toUpperCase())
            .freq(freq)
            .tradeDate(bar.getLocalDate())
            .open(bar.getOpen())
            .high(bar.getHigh())
            .low(bar.getLow())
            .close(bar.getClose())
            .volume(bar.getVolume())
            .amount(bar.getAmount())
            .build();
    }

    private StockKline convertToMinuteKline(String code, String market, String freq, BarData bar, LocalDateTime tradeTime) {
        return StockKline.builder()
            .code(code)
            .market(market.toUpperCase())
            .freq(freq)
            .tradeDate(bar.getLocalDate())
            .tradeTime(tradeTime)
            .open(bar.getOpen())
            .high(bar.getHigh())
            .low(bar.getLow())
            .close(bar.getClose())
            .volume(bar.getVolume())
            .amount(bar.getAmount())
            .build();
    }

    // ==================== 板块数据导入 ====================

    /**
     * 导入板块数据
     *
     * @param fileNames  板块文件名列表
     * @param blockType  板块类型(system/custom)
     * @param importType 导入类型
     * @return 导入结果
     */
    @Transactional
    public ImportResult importBlockData(List<String> fileNames, String blockType, ImportType importType) {
        log.info("导入板块数据: files={}, type={}, importType={}", fileNames.size(), blockType, importType);

        int successCount = 0;
        int failCount = 0;
        long totalRecords = 0;

        // 加载板块名称映射
        Map<String, String> blockNameMap = loadBlockNameMap();

        for (String fileName : fileNames) {
            try {
                String filePath = TDX_INSTALL_DIR + "\\T0002\\blocknew\\" + fileName;
                TdxBlockDataReader.BlockInfo blockInfo = blockDataReader.readBlkFile(filePath, blockNameMap);

                // 如果是全量导入，先删除该板块的旧数据
                if (importType == ImportType.FULL) {
                    stockBlockRepository.deleteByBlockCode(blockInfo.getBlockCode());
                }

                // 导入板块股票
                int importedCount = importBlockStocks(blockInfo, blockType, importType);

                totalRecords += importedCount;
                successCount++;

                log.info("成功导入板块: {} - {} 只股票", blockInfo.getBlockName(), importedCount);

            } catch (Exception e) {
                log.error("导入板块失败: {} - {}", fileName, e.getMessage());
                failCount++;
            }
        }

        return ImportResult.builder()
            .successCount(successCount)
            .failCount(failCount)
            .totalRecords(totalRecords)
            .insertedCount(totalRecords)
            .updatedCount(0L)
            .skippedCount(0L)
            .message(String.format("成功导入 %d 个板块，失败 %d 个，共 %d 只股票",
                successCount, failCount, totalRecords))
            .build();
    }

    /**
     * 导入板块股票
     */
    private int importBlockStocks(TdxBlockDataReader.BlockInfo blockInfo, String blockType, ImportType importType) {
        List<StockBlock> toInsert = new ArrayList<>();
        int insertedCount = 0;

        for (String stockCode : blockInfo.getStockCodes()) {
            // 检查是否已存在
            boolean exists = stockBlockRepository.existsByBlockCodeAndStockCode(
                blockInfo.getBlockCode(), stockCode);

            if (!exists) {
                StockBlock block = StockBlock.builder()
                    .blockCode(blockInfo.getBlockCode())
                    .blockName(blockInfo.getBlockName())
                    .blockType(blockType)
                    .stockCode(stockCode)
                    .build();
                toInsert.add(block);
                insertedCount++;
            }
        }

        // 批量保存
        if (!toInsert.isEmpty()) {
            stockBlockRepository.saveAll(toInsert);
        }

        return insertedCount;
    }

    /**
     * 加载板块名称映射
     */
    private Map<String, String> loadBlockNameMap() {
        try {
            String configPath = TDX_INSTALL_DIR + "\\T0002\\blocknew\\blocknew.cfg";
            File configFile = new File(configPath);
            if (configFile.exists()) {
                return blockConfigReader.readBlockConfig(configPath);
            }
        } catch (Exception e) {
            log.warn("加载板块配置失败: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    // ==================== 备注数据导入 ====================

    /**
     * 导入备注数据
     *
     * @param importType 导入类型
     * @return 导入结果
     */
    @Transactional
    public ImportResult importMarkData(ImportType importType) {
        log.info("导入备注数据: importType={}", importType);

        String markFilePath = TDX_INSTALL_DIR + "\\T0002\\mark.dat";
        File markFile = new File(markFilePath);

        if (!markFile.exists()) {
            return ImportResult.builder()
                .successCount(0)
                .failCount(1)
                .totalRecords(0)
                .message("备注数据文件不存在: " + markFilePath)
                .build();
        }

        try {
            List<TdxMarkDataReader.MarkRecord> records = markDataReader.readMarkFile(markFilePath);

            // 如果是全量导入，先删除所有旧数据
            if (importType == ImportType.FULL) {
                // 这里需要谨慎，可能需要根据业务需求决定是否清空
                log.info("全量导入备注数据，准备清空旧数据...");
                // stockMarkRepository.deleteAll(); // 暂时注释掉，避免误删
            }

            int insertedCount = 0;
            int skippedCount = 0;
            List<StockMark> toInsert = new ArrayList<>();

            for (TdxMarkDataReader.MarkRecord record : records) {
                // 检查是否已存在
                boolean exists = stockMarkRepository.existsByStockCodeAndMarkTypeAndMarkTime(
                    record.getStockCode(), record.getMarkType(), record.getMarkTime());

                if (!exists) {
                    StockMark mark = convertToStockMark(record);
                    toInsert.add(mark);
                    insertedCount++;
                } else {
                    skippedCount++;
                }
            }

            // 批量保存
            if (!toInsert.isEmpty()) {
                stockMarkRepository.saveAll(toInsert);
            }

            log.info("成功导入备注数据: 新增 {} 条，跳过 {} 条", insertedCount, skippedCount);

            return ImportResult.builder()
                .successCount(1)
                .failCount(0)
                .totalRecords((long) records.size())
                .insertedCount((long) insertedCount)
                .updatedCount(0L)
                .skippedCount((long) skippedCount)
                .message(String.format("成功导入备注数据: 新增 %d 条，跳过 %d 条", insertedCount, skippedCount))
                .build();

        } catch (Exception e) {
            log.error("导入备注数据失败: {}", e.getMessage());
            return ImportResult.builder()
                .successCount(0)
                .failCount(1)
                .totalRecords(0)
                .message("导入失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 转换MarkRecord为StockMark实体
     */
    private StockMark convertToStockMark(TdxMarkDataReader.MarkRecord record) {
        return StockMark.builder()
            .stockCode(record.getStockCode())
            .markType(record.getMarkType())
            .markTypeDesc(getMarkTypeDesc(record.getMarkType()))
            .tipWord(record.getTipWord())
            .tip(record.getTip())
            .tipColor(record.getTipColor())
            .colorDesc(getColorDesc(record.getTipColor()))
            .markTime(record.getMarkTime())
            .build();
    }

    /**
     * 获取标记类型描述
     */
    private String getMarkTypeDesc(int markType) {
        switch (markType) {
            case 0: return "自定义标记";
            case 1: return "最近浏览";
            case 2: return "最近关注";
            case 3: return "自选同列";
            case 4: return "持仓股";
            case 5: return "最近浏览(新)";
            default: return "未知类型";
        }
    }

    /**
     * 获取颜色描述
     */
    private String getColorDesc(int color) {
        switch (color) {
            case 0: return "黑色";
            case 1: return "红色";
            case 2: return "绿色";
            case 3: return "黄色";
            case 4: return "蓝色";
            case 5: return "紫色";
            case 6: return "青色";
            case 7: return "白色";
            default: return "未知";
        }
    }
}
