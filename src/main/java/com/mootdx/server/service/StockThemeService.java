package com.mootdx.server.service;

import com.mootdx.server.dto.*;
import com.mootdx.server.entity.*;
import com.mootdx.server.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 股票题材服务类
 * 提供题材导入、查询、搜索等功能
 */
@Service
public class StockThemeService {

    private static final Logger logger = LoggerFactory.getLogger(StockThemeService.class);

    // 默认mark.dat文件路径
    private static final String DEFAULT_MARK_PATH = "C:\\new_tdx\\T0002\\mark.dat";

    @Autowired
    private StockThemeRepository stockThemeRepository;

    @Autowired
    private StockThemeMappingRepository stockThemeMappingRepository;

    @Autowired
    private StockThemeDetailRepository stockThemeDetailRepository;

    @Autowired
    private StockBasicRepository stockBasicRepository;

    /**
     * 从mark.dat文件导入题材数据
     *
     * @param filePaths 文件路径列表，为空则使用默认路径
     * @return 导入结果
     */
    @Transactional
    public ThemeImportResponseDTO importThemes(List<String> filePaths) {
        logger.info("[题材导入] 开始导入题材数据");

        if (filePaths == null || filePaths.isEmpty()) {
            filePaths = Collections.singletonList(DEFAULT_MARK_PATH);
        }

        ThemeImportResponseDTO result = new ThemeImportResponseDTO();
        result.setSuccess(true);

        // 解析所有mark.dat文件
        Map<String, String> stockToThemeName = new HashMap<>(); // 股票代码->题材名称
        Map<String, String> stockToDetail = new HashMap<>(); // 股票代码->详细描述
        Map<String, String> themeNameToColor = new HashMap<>(); // 题材名称->颜色

        for (String filePath : filePaths) {
            parseMarkFile(filePath, stockToThemeName, stockToDetail, themeNameToColor);
        }

        logger.info("[题材导入] 共解析 {} 条股票题材映射, {} 条详细描述, {} 个题材颜色",
                stockToThemeName.size(), stockToDetail.size(), themeNameToColor.size());

        // 构建题材名称到ID的映射
        Map<String, String> themeNameToId = new HashMap<>();

        // 1. 处理题材定义
        Set<String> themeNames = new HashSet<>();
        themeNames.addAll(stockToThemeName.values());

        for (String themeName : themeNames) {
            String themeId = generateThemeId(themeName);
            themeNameToId.put(themeName, themeId);

            Optional<StockTheme> existingTheme = stockThemeRepository.findByThemeId(themeId);
            if (existingTheme.isPresent()) {
                // 更新现有题材
                StockTheme theme = existingTheme.get();
                theme.setColor(themeNameToColor.get(themeName));
                stockThemeRepository.save(theme);
                result.setThemesUpdated(result.getThemesUpdated() + 1);
            } else {
                // 创建新题材
                StockTheme theme = new StockTheme();
                theme.setThemeId(themeId);
                theme.setThemeName(themeName);
                theme.setColor(themeNameToColor.get(themeName));
                theme.setStockCount(0);
                stockThemeRepository.save(theme);
                result.setThemesAdded(result.getThemesAdded() + 1);
            }
        }

        // 2. 处理股票-题材关联
        // 先清除旧的关联数据
        stockThemeMappingRepository.deleteAll();

        for (Map.Entry<String, String> entry : stockToThemeName.entrySet()) {
            String stockCode = entry.getKey();
            String themeName = entry.getValue();
            String themeId = themeNameToId.get(themeName);

            if (themeId == null) {
                continue;
            }

            // 解析股票代码
            String[] parts = parseStockCode(stockCode);
            if (parts == null) {
                continue;
            }

            String market = parts[0];
            String code = parts[1];

            // 创建关联
            StockThemeMapping mapping = new StockThemeMapping();
            mapping.setCode(code);
            mapping.setMarket(market);
            mapping.setThemeId(themeId);
            stockThemeMappingRepository.save(mapping);
            result.setMappingsAdded(result.getMappingsAdded() + 1);
        }

        // 3. 更新每个题材的股票数量
        for (String themeId : themeNameToId.values()) {
            long count = stockThemeMappingRepository.countByThemeId(themeId);
            Optional<StockTheme> themeOpt = stockThemeRepository.findByThemeId(themeId);
            if (themeOpt.isPresent()) {
                StockTheme theme = themeOpt.get();
                theme.setStockCount((int) count);
                stockThemeRepository.save(theme);
            }
        }

        // 4. 处理详细描述
        for (Map.Entry<String, String> entry : stockToDetail.entrySet()) {
            String stockCode = entry.getKey();
            String detailText = entry.getValue();

            String[] parts = parseStockCode(stockCode);
            if (parts == null) {
                continue;
            }

            String market = parts[0];
            String code = parts[1];

            Optional<StockThemeDetail> existingDetail = stockThemeDetailRepository.findByCodeAndMarket(code, market);
            if (existingDetail.isPresent()) {
                StockThemeDetail detail = existingDetail.get();
                detail.setDetailText(detailText);
                stockThemeDetailRepository.save(detail);
                result.setDetailsUpdated(result.getDetailsUpdated() + 1);
            } else {
                StockThemeDetail detail = new StockThemeDetail();
                detail.setCode(code);
                detail.setMarket(market);
                detail.setDetailText(detailText);
                stockThemeDetailRepository.save(detail);
                result.setDetailsAdded(result.getDetailsAdded() + 1);
            }
        }

        // 同步股票基础信息
        syncStockBasicInfo(stockToThemeName.keySet());

        result.setMessage(String.format("导入完成: 新增题材%d个, 更新题材%d个, 关联%d条, 新增描述%d条, 更新描述%d条",
                result.getThemesAdded(), result.getThemesUpdated(), result.getMappingsAdded(),
                result.getDetailsAdded(), result.getDetailsUpdated()));

        logger.info("[题材导入] {}", result.getMessage());
        return result;
    }

    /**
     * 解析mark.dat文件
     */
    private void parseMarkFile(String filePath, Map<String, String> stockToThemeName,
                               Map<String, String> stockToDetail, Map<String, String> themeNameToColor) {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("[题材导入] 文件不存在: {}", filePath);
            return;
        }

        String encoding = detectEncoding(filePath);
        logger.info("[题材导入] 解析文件: {}, 编码: {}", filePath, encoding);

        String currentSection = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.forName(encoding)))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // 检测段落标记
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line;
                    continue;
                }

                // 解析键值对
                int equalIndex = line.indexOf('=');
                if (equalIndex < 0) {
                    continue;
                }

                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();

                // 标准化股票代码
                String stockCode = normalizeStockCode(key);
                if (stockCode == null) {
                    continue;
                }

                if ("[TIPWORD]".equals(currentSection)) {
                    stockToThemeName.put(stockCode, value);
                } else if ("[TIP]".equals(currentSection)) {
                    stockToDetail.put(stockCode, value);
                } else if ("[TIPCOLOR]".equals(currentSection)) {
                    // 颜色格式: 颜色值=题材名称
                    themeNameToColor.put(value, key);
                }
            }

        } catch (IOException e) {
            logger.error("[题材导入] 解析文件失败: {}", filePath, e);
        }
    }

    /**
     * 检测文件编码
     */
    private String detectEncoding(String filePath) {
        String[] encodings = {"GBK", "GB2312", "UTF-8", "ISO-8859-1"};

        for (String encoding : encodings) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), Charset.forName(encoding)))) {
                char[] buffer = new char[1024];
                reader.read(buffer);
                return encoding;
            } catch (Exception e) {
                // 尝试下一个编码
            }
        }

        return "GBK"; // 默认使用GBK
    }

    /**
     * 标准化股票代码
     */
    private String normalizeStockCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }

        code = code.trim();

        // 如果已经是标准格式 SH:xxx 或 SZ:xxx
        if (code.contains(":")) {
            return code.toUpperCase();
        }

        // 提取最后6位数字
        String digits = code.replaceAll("\\D", "");
        if (digits.length() < 6) {
            digits = String.format("%06d", Integer.parseInt(digits));
        } else if (digits.length() > 6) {
            digits = digits.substring(digits.length() - 6);
        }

        // 判断市场
        String market;
        if (digits.startsWith("6") || digits.startsWith("68")) {
            market = "SH";
        } else if (digits.startsWith("0") || digits.startsWith("3") || digits.startsWith("2")) {
            market = "SZ";
        } else if (digits.startsWith("4") || digits.startsWith("8")) {
            market = "BJ";
        } else {
            market = "SH";
        }

        return market + ":" + digits;
    }

    /**
     * 解析股票代码为市场+代码
     */
    private String[] parseStockCode(String stockCode) {
        if (stockCode == null || !stockCode.contains(":")) {
            return null;
        }

        String[] parts = stockCode.split(":");
        if (parts.length != 2) {
            return null;
        }

        return new String[]{parts[0], parts[1]};
    }

    /**
     * 生成题材ID（MD5前16位大写）
     */
    private String generateThemeId(String themeName) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(themeName.getBytes());
            String hex = new BigInteger(1, digest).toString(16).toUpperCase();
            // 补齐32位
            while (hex.length() < 32) {
                hex = "0" + hex;
            }
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // 使用简单的哈希作为备选
            return String.format("%016X", themeName.hashCode());
        }
    }

    /**
     * 同步股票基础信息
     */
    private void syncStockBasicInfo(Set<String> stockCodes) {
        for (String stockCode : stockCodes) {
            String[] parts = parseStockCode(stockCode);
            if (parts == null) {
                continue;
            }

            String market = parts[0];
            String code = parts[1];

            if (!stockBasicRepository.existsByCodeAndMarket(code, market)) {
                StockBasic stockBasic = new StockBasic();
                stockBasic.setCode(code);
                stockBasic.setMarket(market);
                stockBasic.setName(""); // 名称暂时为空
                stockBasicRepository.save(stockBasic);
            }
        }
    }

    /**
     * 获取所有题材列表
     */
    public List<ThemeResponseDTO> getAllThemes() {
        List<StockTheme> themes = stockThemeRepository.findAllByOrderByStockCountDesc();
        return themes.stream().map(this::convertToThemeDTO).collect(Collectors.toList());
    }

    /**
     * 获取热门题材（前20个）
     */
    public List<ThemeResponseDTO> getHotThemes() {
        List<StockTheme> themes = stockThemeRepository.findTop20ByOrderByStockCountDesc();
        return themes.stream().map(this::convertToThemeDTO).collect(Collectors.toList());
    }

    /**
     * 搜索题材
     */
    public List<ThemeResponseDTO> searchThemes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllThemes();
        }

        List<StockTheme> themes = stockThemeRepository.searchByKeyword(keyword.trim());
        return themes.stream().map(this::convertToThemeDTO).collect(Collectors.toList());
    }

    /**
     * 获取题材下的股票列表
     */
    public ThemeStockResponseDTO getThemeStocks(String themeId) {
        Optional<StockTheme> themeOpt = stockThemeRepository.findByThemeId(themeId);
        if (!themeOpt.isPresent()) {
            return null;
        }

        StockTheme theme = themeOpt.get();
        ThemeStockResponseDTO response = new ThemeStockResponseDTO();
        response.setThemeId(theme.getThemeId());
        response.setThemeName(theme.getThemeName());
        response.setThemeKeywords(theme.getThemeKeywords());

        List<StockThemeMapping> mappings = stockThemeMappingRepository.findByThemeId(themeId);
        response.setStockCount(mappings.size());

        List<StockThemeInfoDTO> stocks = new ArrayList<>();
        for (StockThemeMapping mapping : mappings) {
            StockThemeInfoDTO stockInfo = new StockThemeInfoDTO();
            stockInfo.setCode(mapping.getCode());
            stockInfo.setMarket(mapping.getMarket());

            // 获取股票名称
            Optional<StockBasic> stockBasicOpt = stockBasicRepository.findByCodeAndMarket(mapping.getCode(), mapping.getMarket());
            if (stockBasicOpt.isPresent()) {
                stockInfo.setName(stockBasicOpt.get().getName());
                stockInfo.setIndustry(stockBasicOpt.get().getIndustry());
            }

            // 获取详细描述
            Optional<StockThemeDetail> detailOpt = stockThemeDetailRepository.findByCodeAndMarket(mapping.getCode(), mapping.getMarket());
            if (detailOpt.isPresent()) {
                stockInfo.setDetail(detailOpt.get().getDetailText());
            }

            stocks.add(stockInfo);
        }

        response.setStocks(stocks);
        return response;
    }

    /**
     * 获取无题材的股票列表
     */
    public ThemeStockResponseDTO getStocksWithoutTheme() {
        ThemeStockResponseDTO response = new ThemeStockResponseDTO();
        response.setThemeId("NO_THEME");
        response.setThemeName("无题材");

        List<StockBasic> stocksWithoutTheme = stockBasicRepository.findStocksWithoutTheme();
        response.setStockCount(stocksWithoutTheme.size());

        List<StockThemeInfoDTO> stocks = new ArrayList<>();
        for (StockBasic stockBasic : stocksWithoutTheme) {
            StockThemeInfoDTO stockInfo = new StockThemeInfoDTO();
            stockInfo.setCode(stockBasic.getCode());
            stockInfo.setMarket(stockBasic.getMarket());
            stockInfo.setName(stockBasic.getName());
            stockInfo.setIndustry(stockBasic.getIndustry());
            stocks.add(stockInfo);
        }

        response.setStocks(stocks);
        return response;
    }

    /**
     * 获取股票的题材信息
     */
    public StockThemeInfoDTO getStockThemes(String code, String market) {
        StockThemeInfoDTO stockInfo = new StockThemeInfoDTO();
        stockInfo.setCode(code);
        stockInfo.setMarket(market.toUpperCase());

        // 获取股票基本信息
        Optional<StockBasic> stockBasicOpt = stockBasicRepository.findByCodeAndMarket(code, market.toUpperCase());
        if (stockBasicOpt.isPresent()) {
            StockBasic stockBasic = stockBasicOpt.get();
            stockInfo.setName(stockBasic.getName());
            stockInfo.setIndustry(stockBasic.getIndustry());
        }

        // 获取题材列表
        List<StockThemeMapping> mappings = stockThemeMappingRepository.findByCodeAndMarket(code, market.toUpperCase());
        List<ThemeResponseDTO> themes = new ArrayList<>();

        for (StockThemeMapping mapping : mappings) {
            Optional<StockTheme> themeOpt = stockThemeRepository.findByThemeId(mapping.getThemeId());
            if (themeOpt.isPresent()) {
                themes.add(convertToThemeDTO(themeOpt.get()));
            }
        }
        stockInfo.setThemes(themes);

        // 获取详细描述
        Optional<StockThemeDetail> detailOpt = stockThemeDetailRepository.findByCodeAndMarket(code, market.toUpperCase());
        if (detailOpt.isPresent()) {
            stockInfo.setDetail(detailOpt.get().getDetailText());
        }

        return stockInfo;
    }

    /**
     * 获取默认导入路径
     */
    public ImportPathResponseDTO getDefaultImportPath() {
        File file = new File(DEFAULT_MARK_PATH);
        ImportPathResponseDTO response = new ImportPathResponseDTO();
        response.setDefaultPath(DEFAULT_MARK_PATH);
        response.setExists(file.exists());
        response.setMessage(file.exists() ? "文件存在" : "文件不存在");
        return response;
    }

    /**
     * 转换为ThemeResponseDTO
     */
    private ThemeResponseDTO convertToThemeDTO(StockTheme theme) {
        ThemeResponseDTO dto = new ThemeResponseDTO();
        dto.setThemeId(theme.getThemeId());
        dto.setThemeName(theme.getThemeName());
        dto.setThemeKeywords(theme.getThemeKeywords());
        dto.setStockCount(theme.getStockCount());
        dto.setColor(theme.getColor());
        return dto;
    }
}
