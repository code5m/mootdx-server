package com.mootdx.server.controller;

import com.mootdx.localdata.TdxBlockConfigReader;
import com.mootdx.localdata.TdxBlockDataReader;
import com.mootdx.localdata.TdxLocalDataReader;
import com.mootdx.localdata.TdxMarkDataReader;
import com.mootdx.model.BarData;
import com.mootdx.server.entity.StockKline;
import com.mootdx.server.repository.StockBlockRepository;
import com.mootdx.server.repository.StockKlineRepository;
import com.mootdx.server.service.TdxDataImportService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通达信本地数据导入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tdx-import")
public class TdxDataImportController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TdxDataImportService importService;

    @Autowired
    private StockKlineRepository stockKlineRepository;

    @Autowired
    private StockBlockRepository stockBlockRepository;

    private static final String TDX_INSTALL_DIR = "C:\\new_tdx";

    /**
     * 获取数据库统计信息
     */
    @GetMapping("/db-stats")
    public ResponseEntity<Map<String, Object>> getDbStats() {
        log.info("获取数据库统计信息");

        Map<String, Object> stats = new HashMap<>();

        // 查询各表的记录数
        long totalKline = stockKlineRepository.count();
        long shDayCount = stockKlineRepository.countByMarketAndFreq("SH", "day");
        long szDayCount = stockKlineRepository.countByMarketAndFreq("SZ", "day");
        long bjDayCount = stockKlineRepository.countByMarketAndFreq("BJ", "day");

        stats.put("totalKline", totalKline);
        stats.put("shDay", shDayCount);
        stats.put("szDay", szDayCount);
        stats.put("bjDay", bjDayCount);

        return ResponseEntity.ok(stats);
    }

    /**
     * 获取数据文件统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<DataStatistics> getStatistics() {
        log.info("获取通达信数据文件统计信息");

        DataStatistics stats = new DataStatistics();

        // 统计K线数据
        stats.setShDayFiles(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\sh\\lday", ".day"));
        stats.setSzDayFiles(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\sz\\lday", ".day"));
        stats.setBjDayFiles(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\bj\\lday", ".day"));

        // 统计1分钟线数据
        stats.setShMin1Files(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\sh\\minline", ".lc1"));
        stats.setSzMin1Files(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\sz\\minline", ".lc1"));

        // 统计5分钟线数据
        stats.setShMin5Files(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\sh\\fzline", ".lc5"));
        stats.setSzMin5Files(countFiles(TDX_INSTALL_DIR + "\\vipdoc\\sz\\fzline", ".lc5"));

        // 统计板块数据（区分系统板块和自定义板块）
        List<FileInfo> blockFiles = scanBlockFiles(TDX_INSTALL_DIR + "\\T0002\\blocknew");
        stats.setBlockFiles(blockFiles.size());
        stats.setSystemBlockFiles((int) blockFiles.stream().filter(f -> "system".equals(f.getBlockType())).count());
        stats.setCustomBlockFiles((int) blockFiles.stream().filter(f -> "custom".equals(f.getBlockType())).count());

        // 统计备注数据
        File markFile = new File(TDX_INSTALL_DIR + "\\T0002\\mark.dat");
        stats.setMarkFileExists(markFile.exists());
        stats.setMarkFileSize(markFile.exists() ? markFile.length() : 0);

        // 计算总大小
        stats.setTotalKlineSize(calculateTotalSize(TDX_INSTALL_DIR + "\\vipdoc", ".day"));
        stats.setTotalBlockSize(calculateTotalSize(TDX_INSTALL_DIR + "\\T0002\\blocknew", ".blk"));

        return ResponseEntity.ok(stats);
    }

    /**
     * 扫描数据文件
     */
    @GetMapping("/scan")
    public ResponseEntity<List<FileInfo>> scanFiles(@RequestParam String type) {
        log.info("扫描数据文件: type={}", type);

        List<FileInfo> files = new ArrayList<>();
        TdxLocalDataReader reader = new TdxLocalDataReader();

        switch (type) {
            case "sh-day":
                files = scanDayFiles(TDX_INSTALL_DIR + "\\vipdoc\\sh\\lday", "sh");
                break;
            case "sz-day":
                files = scanDayFiles(TDX_INSTALL_DIR + "\\vipdoc\\sz\\lday", "sz");
                break;
            case "bj-day":
                files = scanDayFiles(TDX_INSTALL_DIR + "\\vipdoc\\bj\\lday", "bj");
                break;
            case "block":
                files = scanBlockFiles(TDX_INSTALL_DIR + "\\T0002\\blocknew");
                break;
            case "system-block":
                files = scanBlockFiles(TDX_INSTALL_DIR + "\\T0002\\blocknew").stream()
                    .filter(f -> "system".equals(f.getBlockType()))
                    .collect(Collectors.toList());
                break;
            case "custom-block":
                files = scanBlockFiles(TDX_INSTALL_DIR + "\\T0002\\blocknew").stream()
                    .filter(f -> "custom".equals(f.getBlockType()))
                    .collect(Collectors.toList());
                break;
            case "mark":
                files = scanMarkFile(TDX_INSTALL_DIR + "\\T0002\\mark.dat");
                break;
            case "sh-min1":
                files = scanMinuteFiles(TDX_INSTALL_DIR + "\\vipdoc\\sh\\minline", "sh", "1min");
                break;
            case "sz-min1":
                files = scanMinuteFiles(TDX_INSTALL_DIR + "\\vipdoc\\sz\\minline", "sz", "1min");
                break;
            case "sh-min5":
                files = scanMinuteFiles(TDX_INSTALL_DIR + "\\vipdoc\\sh\\fzline", "sh", "5min");
                break;
            case "sz-min5":
                files = scanMinuteFiles(TDX_INSTALL_DIR + "\\vipdoc\\sz\\fzline", "sz", "5min");
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(files);
    }

    /**
     * 导入日线数据
     * 支持全量导入、增量导入和差异导入
     */
    @PostMapping("/import/day")
    public ResponseEntity<TdxDataImportService.ImportResult> importDayData(@RequestBody ImportRequest request) {
        log.info("导入日线数据: market={}, files={}, importType={}",
            request.getMarket(), request.getFiles().size(), request.getImportType());

        // 根据导入类型选择策略
        TdxDataImportService.ImportType importType = parseImportType(request.getImportType());

        TdxDataImportService.ImportResult result = importService.importDayData(request.getMarket(), request.getFiles(), importType);
        return ResponseEntity.ok(result);
    }

    /**
     * 比对日线数据差异
     */
    @GetMapping("/compare/day")
    public ResponseEntity<List<DataDiffInfo>> compareDayData(
            @RequestParam String market,
            @RequestParam String fileName) {
        log.info("比对日线数据差异: market={}, file={}", market, fileName);

        try {
            List<TdxDataImportService.DataDiff> diffs = importService.compareDayData(market, fileName);

            List<DataDiffInfo> result = diffs.stream()
                .map(this::convertToDiffInfo)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("比对失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取股票最新数据日期
     */
    @GetMapping("/latest-date")
    public ResponseEntity<LatestDateInfo> getLatestDate(
            @RequestParam String code,
            @RequestParam String market,
            @RequestParam String freq) {
        log.info("获取最新数据日期: code={}, market={}, freq={}", code, market, freq);

        java.time.LocalDate latestDate = importService.getLatestTradeDate(code, market, freq);

        LatestDateInfo info = new LatestDateInfo();
        info.setCode(code);
        info.setMarket(market);
        info.setFreq(freq);
        info.setLatestDate(latestDate != null ? latestDate.toString() : null);

        return ResponseEntity.ok(info);
    }

    /**
     * 比对分钟线数据差异
     */
    @GetMapping("/compare/minute")
    public ResponseEntity<List<DataDiffInfo>> compareMinuteData(
            @RequestParam String market,
            @RequestParam String minuteType,
            @RequestParam String fileName) {
        log.info("比对分钟线数据: market={}, type={}, file={}", market, minuteType, fileName);

        try {
            List<TdxDataImportService.DataDiff> diffs = importService.compareMinuteData(market, minuteType, fileName);

            List<DataDiffInfo> result = diffs.stream()
                .map(this::convertToDiffInfo)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("比对分钟线数据失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 导入分钟线数据
     * 支持全量导入、增量导入和差异导入
     */
    @PostMapping("/import/minute")
    public ResponseEntity<TdxDataImportService.ImportResult> importMinuteData(@RequestBody ImportRequest request) {
        log.info("导入分钟线数据: market={}, type={}, files={}, importType={}",
            request.getMarket(), request.getMinuteType(), request.getFiles().size(), request.getImportType());

        // 根据导入类型选择策略
        TdxDataImportService.ImportType importType = parseImportType(request.getImportType());

        TdxDataImportService.ImportResult result = importService.importMinuteData(
            request.getMarket(), request.getMinuteType(), request.getFiles(), importType);

        return ResponseEntity.ok(result);
    }

    /**
     * 导入板块数据
     * 支持全量导入、增量导入
     */
    @PostMapping("/import/block")
    public ResponseEntity<TdxDataImportService.ImportResult> importBlockData(@RequestBody ImportRequest request) {
        log.info("导入板块数据: files={}, type={}, importType={}",
            request.getFiles().size(), request.getBlockType(), request.getImportType());

        // 根据导入类型选择策略
        TdxDataImportService.ImportType importType = parseImportType(request.getImportType());

        TdxDataImportService.ImportResult result = importService.importBlockData(
            request.getFiles(), request.getBlockType(), importType);

        return ResponseEntity.ok(result);
    }

    /**
     * 导入备注数据
     * 支持全量导入、增量导入
     */
    @PostMapping("/import/mark")
    public ResponseEntity<TdxDataImportService.ImportResult> importMarkData(@RequestBody ImportRequest request) {
        log.info("导入备注数据: importType={}", request.getImportType());

        // 根据导入类型选择策略
        TdxDataImportService.ImportType importType = parseImportType(request.getImportType());

        TdxDataImportService.ImportResult result = importService.importMarkData(importType);

        return ResponseEntity.ok(result);
    }

    /**
     * 解析单个文件(预览)
     */
    @GetMapping("/preview")
    public ResponseEntity<PreviewResult> previewFile(@RequestParam String type, @RequestParam String fileName) {
        log.info("预览文件: type={}, fileName={}", type, fileName);

        PreviewResult result = new PreviewResult();

        try {
            if ("day".equals(type)) {
                // 从文件名提取市场
                String market = fileName.startsWith("sh") ? "sh" : (fileName.startsWith("sz") ? "sz" : "bj");
                String filePath = TDX_INSTALL_DIR + "\\vipdoc\\" + market + "\\lday\\" + fileName;

                TdxLocalDataReader reader = new TdxLocalDataReader();
                List<BarData> bars = reader.readDayFile(filePath);

                result.setFileName(fileName);
                result.setRecordCount(bars.size());
                result.setType("日线数据");

                // 取前5条和后5条数据
                List<Map<String, Object>> previewData = new ArrayList<>();
                int count = Math.min(5, bars.size());
                for (int i = 0; i < count; i++) {
                    previewData.add(convertBarData(bars.get(i)));
                }
                if (bars.size() > 10) {
                    previewData.add(createSeparator());
                }
                for (int i = Math.max(count, bars.size() - 5); i < bars.size(); i++) {
                    previewData.add(convertBarData(bars.get(i)));
                }
                result.setData(previewData);

            } else if ("block".equals(type)) {
                String filePath = TDX_INSTALL_DIR + "\\T0002\\blocknew\\" + fileName;

                TdxBlockDataReader reader = new TdxBlockDataReader();
                TdxBlockDataReader.BlockInfo block = reader.readBlkFile(filePath);

                result.setFileName(fileName);
                result.setRecordCount(block.getStockCount());
                result.setType("板块数据");

                List<Map<String, Object>> previewData = new ArrayList<>();
                List<String> stocks = block.getStockCodes();
                int count = Math.min(20, stocks.size());
                for (int i = 0; i < count; i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("index", i + 1);
                    item.put("code", stocks.get(i));
                    previewData.add(item);
                }
                result.setData(previewData);
            } else if ("mark".equals(type)) {
                String filePath = TDX_INSTALL_DIR + "\\T0002\\mark.dat";

                TdxMarkDataReader reader = new TdxMarkDataReader();
                List<TdxMarkDataReader.MarkRecord> records = reader.readMarkFile(filePath);

                result.setFileName("mark.dat");
                result.setRecordCount(records.size());
                result.setType("备注数据");

                List<Map<String, Object>> previewData = new ArrayList<>();
                int count = Math.min(20, records.size());
                for (int i = 0; i < count; i++) {
                    TdxMarkDataReader.MarkRecord record = records.get(i);
                    Map<String, Object> item = new HashMap<>();
                    item.put("index", i + 1);
                    item.put("code", record.getStockCode());
                    item.put("markType", record.getMarkType());
                    item.put("markTypeDesc", reader.getMarkTypeDescription(record.getMarkType()));
                    item.put("tip", record.getTip());
                    item.put("tipWord", record.getTipWord());
                    item.put("color", record.getTipColor());
                    item.put("colorDesc", reader.getColorDescription(record.getTipColor()));
                    item.put("time", record.getMarkTime());
                    previewData.add(item);
                }
                result.setData(previewData);
            } else if ("min1".equals(type) || "min5".equals(type)) {
                // 从文件名提取市场
                String market = fileName.startsWith("sh") ? "sh" : (fileName.startsWith("sz") ? "sz" : "bj");
                String subDir = "min1".equals(type) ? "minline" : "fzline";
                String filePath = TDX_INSTALL_DIR + "\\vipdoc\\" + market + "\\" + subDir + "\\" + fileName;

                TdxLocalDataReader reader = new TdxLocalDataReader();
                List<BarData> bars;

                if ("min1".equals(type)) {
                    bars = reader.readMinute1File(filePath);
                    result.setType("1分钟线数据");
                } else {
                    bars = reader.readMinute5File(filePath);
                    result.setType("5分钟线数据");
                }

                result.setFileName(fileName);
                result.setRecordCount(bars.size());

                // 取前5条和后5条数据
                List<Map<String, Object>> previewData = new ArrayList<>();
                int count = Math.min(5, bars.size());
                for (int i = 0; i < count; i++) {
                    previewData.add(convertBarData(bars.get(i)));
                }
                if (bars.size() > 10) {
                    previewData.add(createSeparator());
                }
                for (int i = Math.max(count, bars.size() - 5); i < bars.size(); i++) {
                    previewData.add(convertBarData(bars.get(i)));
                }
                result.setData(previewData);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("预览失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 私有方法 ====================

    private int countFiles(String directory, String extension) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(extension));
        return files != null ? files.length : 0;
    }

    private long calculateTotalSize(String baseDir, String extension) {
        long totalSize = 0;
        File dir = new File(baseDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(extension)) {
                        totalSize += file.length();
                    } else if (file.isDirectory()) {
                        totalSize += calculateTotalSize(file.getAbsolutePath(), extension);
                    }
                }
            }
        }
        return totalSize;
    }

    private List<FileInfo> scanDayFiles(String directory, String market) {
        List<FileInfo> files = new ArrayList<>();
        TdxLocalDataReader reader = new TdxLocalDataReader();

        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }

        File[] fileArray = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".day"));
        if (fileArray != null) {
            // 第一步：收集所有文件信息
            for (File file : fileArray) {
                FileInfo info = new FileInfo();
                String stockCode = reader.extractStockCode(file.getName());
                info.setFileName(file.getName());
                info.setStockCode(stockCode);
                info.setMarket(market);
                info.setSize(file.length());
                int fileRecordCount = (int) (file.length() / 32);
                info.setRecordCount(fileRecordCount);
                files.add(info);
            }

            // 第二步：批量查询数据库（性能优化）
            enrichDbInfoBatch(files, market, "day");
        }

        return files;
    }

    /**
     * 补充数据库信息 - 优化版本，使用count查询而非查询所有数据
     */
    private void enrichDbInfo(FileInfo info, String stockCode, String market, String freq) {
        try {
            // 只查询数量，不查询所有记录（性能优化）
            long dbRecordCount = stockKlineRepository.countByCodeAndMarketAndFreq(stockCode, market.toUpperCase(), freq);
            info.setDbRecordCount((int) dbRecordCount);

            // 计算差异（取绝对值，表示文件和数据库的差异数量）
            int diffCount = info.getRecordCount() - (int) dbRecordCount;
            info.setDiffCount(Math.abs(diffCount));

            // 获取最新日期（只在有数据时查询）
            if (dbRecordCount > 0) {
                Optional<StockKline> latest = stockKlineRepository.findTopByCodeAndMarketAndFreqOrderByTradeDateDescTradeTimeDesc(
                    stockCode, market.toUpperCase(), freq);
                latest.ifPresent(k -> info.setDbLatestDate(k.getTradeDate().format(DateTimeFormatter.ISO_DATE)));
            }

            // 设置状态
            if (dbRecordCount == 0) {
                info.setStatus("NEW");  // 全新，数据库中没有
            } else if (diffCount == 0) {
                info.setStatus("SYNC"); // 已同步，数量一致
            } else {
                info.setStatus("DIFF"); // 有差异（文件多或数据库多）
            }
        } catch (Exception e) {
            log.warn("查询数据库信息失败: {} - {}", stockCode, e.getMessage());
            info.setStatus("UNKNOWN");
        }
    }

    /**
     * 批量补充数据库信息 - 分批查询避免IN子句过长（性能优化）
     */
    private void enrichDbInfoBatch(List<FileInfo> files, String market, String freq) {
        if (files.isEmpty()) {
            return;
        }

        // 分批处理，每批500个股票（避免IN子句过长）
        int batchSize = 500;
        Map<String, Long> countMap = new HashMap<>();
        Map<String, LocalDate> latestDateMap = new HashMap<>();

        List<String> allStockCodes = files.stream()
            .map(FileInfo::getStockCode)
            .collect(Collectors.toList());

        int totalBatches = (allStockCodes.size() + batchSize - 1) / batchSize;
        log.info("开始批量查询: {} 个文件，分 {} 批处理，市场={}，频率={}",
            files.size(), totalBatches, market, freq);

        for (int i = 0; i < allStockCodes.size(); i += batchSize) {
            List<String> batch = allStockCodes.subList(i, Math.min(i + batchSize, allStockCodes.size()));
            int batchNum = i / batchSize + 1;

            try {
                // 批量查询记录数
                List<Object[]> results = stockKlineRepository.countByCodeListAndMarketAndFreq(
                    batch, market.toUpperCase(), freq);
                for (Object[] row : results) {
                    countMap.put((String) row[0], (Long) row[1]);
                }

                // 批量查询最新日期
                List<Object[]> latestResults = stockKlineRepository.findLatestDateByCodeList(
                    batch, market.toUpperCase(), freq);
                for (Object[] row : latestResults) {
                    String code = (String) row[0];
                    LocalDate date = (LocalDate) row[1];
                    latestDateMap.merge(code, date, (a, b) -> a.isAfter(b) ? a : b);
                }

                log.debug("第 {}/{} 批查询完成: {} 个股票", batchNum, totalBatches, batch.size());

            } catch (Exception e) {
                log.error("第 {} 批查询失败: {}", batchNum, e.getMessage());
                // 降级为逐个查询这一批
                for (String code : batch) {
                    FileInfo info = files.stream()
                        .filter(f -> f.getStockCode().equals(code))
                        .findFirst()
                        .orElse(null);
                    if (info != null) {
                        enrichDbInfo(info, code, market, freq);
                    }
                }
            }
        }

        // 填充所有文件的数据库信息
        for (FileInfo info : files) {
            String stockCode = info.getStockCode();
            long dbRecordCount = countMap.getOrDefault(stockCode, 0L);
            info.setDbRecordCount((int) dbRecordCount);

            // 计算差异
            int diffCount = info.getRecordCount() - (int) dbRecordCount;
            info.setDiffCount(Math.abs(diffCount));

            // 设置最新日期
            LocalDate latestDate = latestDateMap.get(stockCode);
            if (latestDate != null) {
                info.setDbLatestDate(latestDate.format(DateTimeFormatter.ISO_DATE));
            }

            // 设置状态
            if (dbRecordCount == 0) {
                info.setStatus("NEW");
            } else if (diffCount == 0) {
                info.setStatus("SYNC");
            } else {
                info.setStatus("DIFF");
            }
        }

        log.info("批量查询完成: {} 个文件，市场={}，频率={}", files.size(), market, freq);
    }

    private List<FileInfo> scanBlockFiles(String directory) {
        List<FileInfo> files = new ArrayList<>();
        TdxBlockDataReader reader = new TdxBlockDataReader();

        // 读取板块名称配置文件
        Map<String, String> blockNameMap = loadBlockNameMap();

        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }

        File[] fileArray = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".blk"));
        if (fileArray != null) {
            for (File file : fileArray) {
                try {
                    TdxBlockDataReader.BlockInfo block = reader.readBlkFile(file.getAbsolutePath(), blockNameMap);

                    FileInfo info = new FileInfo();
                    info.setFileName(file.getName());
                    info.setBlockName(block.getBlockName());
                    info.setStockCount(block.getStockCount());
                    info.setSize(file.length());
                    // 判断板块类型：系统板块以数字开头（如8801），自定义板块为其他名称
                    String blockCode = block.getBlockCode();
                    boolean isSystemBlock = blockCode.matches("^\\d+.*");
                    info.setBlockType(isSystemBlock ? "system" : "custom");

                    // 查询数据库状态
                    enrichBlockDbInfo(info, blockCode, block.getStockCount());

                    files.add(info);
                } catch (IOException e) {
                    log.warn("读取板块文件失败: {}", file.getName());
                }
            }
        }

        return files;
    }

    /**
     * 补充板块数据库信息
     */
    private void enrichBlockDbInfo(FileInfo info, String blockCode, int fileStockCount) {
        try {
            // 查询数据库中该板块的股票数量
            long dbStockCount = stockBlockRepository.countByBlockCode(blockCode);
            info.setDbRecordCount((int) dbStockCount);

            // 计算差异
            int diffCount = fileStockCount - (int) dbStockCount;
            info.setDiffCount(Math.abs(diffCount));

            // 设置状态
            if (dbStockCount == 0) {
                info.setStatus("NEW");  // 全新，数据库中没有
            } else if (diffCount == 0) {
                info.setStatus("SYNC"); // 已同步，数量一致
            } else {
                info.setStatus("DIFF"); // 有差异
            }
        } catch (Exception e) {
            log.warn("查询板块数据库信息失败: {} - {}", blockCode, e.getMessage());
            info.setStatus("UNKNOWN");
        }
    }

    /**
     * 加载板块名称映射配置
     * @return 文件名到板块显示名称的映射
     */
    private Map<String, String> loadBlockNameMap() {
        try {
            TdxBlockConfigReader configReader = new TdxBlockConfigReader();
            return configReader.readBlockConfig(TDX_INSTALL_DIR + "\\T0002\\blocknew\\blocknew.cfg");
        } catch (IOException e) {
            log.warn("读取板块配置文件失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private List<FileInfo> scanMarkFile(String filePath) {
        List<FileInfo> files = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            return files;
        }

        try {
            TdxMarkDataReader reader = new TdxMarkDataReader();
            List<TdxMarkDataReader.MarkRecord> records = reader.readMarkFile(filePath);

            FileInfo info = new FileInfo();
            info.setFileName("mark.dat");
            info.setRecordCount(records.size());
            info.setSize(file.length());
            info.setMarket("remark");
            files.add(info);

        } catch (IOException e) {
            log.warn("读取备注数据文件失败: {}", filePath);
        }

        return files;
    }

    private List<FileInfo> scanMinuteFiles(String directory, String market, String minuteType) {
        List<FileInfo> files = new ArrayList<>();
        TdxLocalDataReader reader = new TdxLocalDataReader();

        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }

        String extension = "1min".equals(minuteType) ? ".lc1" : ".lc5";
        File[] fileArray = dir.listFiles((d, name) -> name.toLowerCase().endsWith(extension));
        if (fileArray != null) {
            for (File file : fileArray) {
                FileInfo info = new FileInfo();
                info.setFileName(file.getName());
                info.setStockCode(reader.extractStockCode(file.getName()));
                info.setMarket(market);
                info.setMinuteType(minuteType);
                info.setSize(file.length());
                info.setRecordCount((int) (file.length() / 32));
                files.add(info);
            }
        }

        return files;
    }

    private Map<String, Object> convertBarData(BarData bar) {
        Map<String, Object> map = new HashMap<>();
        map.put("date", bar.getDate());
        map.put("open", bar.getOpen());
        map.put("high", bar.getHigh());
        map.put("low", bar.getLow());
        map.put("close", bar.getClose());
        map.put("volume", bar.getVolume());
        map.put("amount", bar.getAmount());
        return map;
    }

    private Map<String, Object> createSeparator() {
        Map<String, Object> map = new HashMap<>();
        map.put("separator", true);
        map.put("text", "...");
        return map;
    }

    // ==================== DTO类 ====================

    @Data
    public static class DataStatistics {
        private int shDayFiles;
        private int szDayFiles;
        private int bjDayFiles;
        private int shMin1Files;
        private int szMin1Files;
        private int shMin5Files;
        private int szMin5Files;
        private int blockFiles;           // 总板块数
        private int systemBlockFiles;     // 系统板块数
        private int customBlockFiles;     // 自定义板块数
        private boolean markFileExists;
        private long markFileSize;
        private long totalKlineSize;
        private long totalBlockSize;

        public int getTotalDayFiles() {
            return shDayFiles + szDayFiles + bjDayFiles;
        }

        public int getTotalMin1Files() {
            return shMin1Files + szMin1Files;
        }

        public int getTotalMin5Files() {
            return shMin5Files + szMin5Files;
        }
    }

    @Data
    public static class FileInfo {
        private String fileName;
        private String stockCode;
        private String market;
        private String minuteType;
        private String blockName;
        private String blockType;  // system=系统板块, custom=自定义板块
        private long size;
        private int recordCount;
        private int stockCount;

        // 数据库对比信息
        private Integer dbRecordCount;      // 数据库中的记录数
        private String dbLatestDate;        // 数据库最新日期
        private Integer diffCount;          // 差异数量（文件比数据库多的记录数）
        private String status;              // 状态：NEW(新增), SYNC(已同步), DIFF(有差异)
    }

    @Data
    public static class ImportRequest {
        private String market;
        private String minuteType;
        private String blockType;   // system=系统板块, custom=自定义板块
        private List<String> files;
        private String importType;  // FULL=全量, INCREMENTAL=增量, DIFFERENTIAL=差异
    }

    /**
     * 解析导入类型
     */
    private TdxDataImportService.ImportType parseImportType(String importType) {
        if (importType == null) {
            return TdxDataImportService.ImportType.INCREMENTAL;  // 默认增量导入
        }
        switch (importType.toUpperCase()) {
            case "FULL":
                return TdxDataImportService.ImportType.FULL;
            case "DIFFERENTIAL":
            case "DIFF":
                return TdxDataImportService.ImportType.DIFFERENTIAL;
            case "INCREMENTAL":
            case "INC":
            default:
                return TdxDataImportService.ImportType.INCREMENTAL;
        }
    }

    /**
     * 转换差异信息为DTO
     */
    private DataDiffInfo convertToDiffInfo(TdxDataImportService.DataDiff diff) {
        DataDiffInfo info = new DataDiffInfo();
        info.setCode(diff.getCode());
        info.setMarket(diff.getMarket());
        info.setTradeDate(diff.getTradeDate().toString());
        info.setDiffType(diff.getDiffType());

        if (diff.getFileData() != null) {
            info.setFileOpen(diff.getFileData().getOpen().doubleValue());
            info.setFileHigh(diff.getFileData().getHigh().doubleValue());
            info.setFileLow(diff.getFileData().getLow().doubleValue());
            info.setFileClose(diff.getFileData().getClose().doubleValue());
            info.setFileVolume((double) diff.getFileData().getVolume());
            info.setFileAmount(diff.getFileData().getAmount().doubleValue());
        }

        if (diff.getDbData() != null) {
            info.setDbOpen(diff.getDbData().getOpen().doubleValue());
            info.setDbHigh(diff.getDbData().getHigh().doubleValue());
            info.setDbLow(diff.getDbData().getLow().doubleValue());
            info.setDbClose(diff.getDbData().getClose().doubleValue());
            info.setDbVolume(diff.getDbData().getVolume());
            info.setDbAmount(diff.getDbData().getAmount().doubleValue());
        }

        return info;
    }



    @Data
    public static class PreviewResult {
        private String fileName;
        private String type;
        private int recordCount;
        private List<Map<String, Object>> data;
    }

    /**
     * 数据差异信息DTO
     */
    @Data
    public static class DataDiffInfo {
        private String code;           // 股票代码
        private String market;         // 市场
        private String tradeDate;      // 交易日期
        private String diffType;       // 差异类型：NEW(新增), MODIFIED(修改), SAME(相同)

        // 文件数据
        private Double fileOpen;
        private Double fileHigh;
        private Double fileLow;
        private Double fileClose;
        private Double fileVolume;
        private Double fileAmount;

        // 数据库数据
        private Double dbOpen;
        private Double dbHigh;
        private Double dbLow;
        private Double dbClose;
        private Long dbVolume;
        private Double dbAmount;
    }

    /**
     * 最新日期信息DTO
     */
    @Data
    public static class LatestDateInfo {
        private String code;
        private String market;
        private String freq;
        private String latestDate;
    }

    /**
     * 简单导入结果DTO（用于板块和备注导入）
     */
    @Data
    public static class SimpleImportResult {
        private int successCount;
        private int failCount;
        private long totalRecords;
        private String message;
    }
}
