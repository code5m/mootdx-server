package com.mootdx.server.service;

import com.mootdx.model.CompanyInfo;
import com.mootdx.server.dto.CompanyInfoDTO;
import com.mootdx.server.dto.ShareholderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F10公司资料解析服务
 * 将原始文本内容解析为结构化的DTO对象
 *
 * @author MooTDX
 * @since 1.0.0
 */
@Slf4j
@Service
public class CompanyInfoParserService {

    // 日期格式解析器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER2 = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 将CompanyInfo模型转换为增强的DTO
     *
     * @param companyInfo 原始公司信息
     * @return 结构化的DTO
     */
    public CompanyInfoDTO parseCompanyInfo(CompanyInfo companyInfo) {
        if (companyInfo == null) {
            return null;
        }

        log.info("[parseCompanyInfo] 开始解析公司资料: code={}", companyInfo.getCode());

        CompanyInfoDTO.CompanyInfoDTOBuilder builder = CompanyInfoDTO.builder();

        // 基础信息
        builder.code(companyInfo.getCode())
               .name(companyInfo.getName())
               .market(companyInfo.getMarket())
               .fullName(companyInfo.getFullName())
               .englishName(companyInfo.getEnglishName())
               .industry(companyInfo.getIndustry())
               .website(companyInfo.getWebsite())
               .listingDate(companyInfo.getListingDate());

        // 股本信息
        builder.totalShares(companyInfo.getTotalShares())
               .circulatingShares(companyInfo.getCirculatingShares())
               .totalSharesFormatted(CompanyInfoDTO.formatAmount(companyInfo.getTotalShares()))
               .circulatingSharesFormatted(CompanyInfoDTO.formatAmount(companyInfo.getCirculatingShares()));

        // 财务指标
        builder.eps(companyInfo.getEps())
               .navps(companyInfo.getNavps())
               .roe(companyInfo.getRoe())
               .revenuePerShare(companyInfo.getRevenuePerShare())
               .operatingCashFlowPerShare(companyInfo.getOperatingCashFlowPerShare())
               .grossProfitMargin(companyInfo.getGrossProfitMargin())
               .netProfitMargin(companyInfo.getNetProfitMargin())
               .debtToAssetRatio(companyInfo.getDebtToAssetRatio())
               .peRatio(companyInfo.getPeRatio())
               .pbRatio(companyInfo.getPbRatio());

        // 联系信息
        builder.registeredAddress(companyInfo.getRegisteredAddress())
               .officeAddress(companyInfo.getOfficeAddress())
               .phone(companyInfo.getPhone())
               .fax(companyInfo.getFax())
               .email(companyInfo.getEmail())
               .postalCode(companyInfo.getPostalCode());

        // 股东信息
        builder.totalShareholders(companyInfo.getTotalShareholders())
               .institutionalShareholders(companyInfo.getInstitutionalShareholders());

        // 分红信息
        builder.dividendPerShare(companyInfo.getDividendPerShare())
               .dividendYield(companyInfo.getDividendYield())
               .lastDividendDate(companyInfo.getLastDividendDate());

        // 公司概况
        builder.companyProfile(companyInfo.getCompanyProfile())
               .businessScope(companyInfo.getBusinessScope());

        // 解析原始内容中的额外信息
        if (companyInfo.getCompanyProfile() != null) {
            parseContentDetails(builder, companyInfo.getCompanyProfile());
        }

        CompanyInfoDTO result = builder.build();
        log.info("[parseCompanyInfo] 解析完成: code={}, name={}", result.getCode(), result.getName());

        return result;
    }

    /**
     * 解析内容详情，提取更多结构化信息
     */
    private void parseContentDetails(CompanyInfoDTO.CompanyInfoDTOBuilder builder, String content) {
        log.debug("[parseContentDetails] 解析内容详情, 内容长度: {}", content.length());

        // 提取主营业务
        String mainBusiness = extractField(content, "主营业务", "经营范围");
        if (mainBusiness != null) {
            builder.mainBusiness(mainBusiness);
        }

        // 提取公司简介（如果没有的话）
        if (content.contains("公司概况") || content.contains("公司简介")) {
            String profile = extractSection(content, "公司概况", "主营业务", "经营范围");
            if (profile == null) {
                profile = extractSection(content, "公司简介", "主营业务", "经营范围");
            }
            if (profile != null && profile.length() > 10) {
                builder.companyProfile(profile);
            }
        }

        // 提取十大股东
        List<ShareholderDTO> shareholders = parseShareholders(content);
        if (!shareholders.isEmpty()) {
            builder.top10Shareholders(shareholders);
        }

        // 解析所有分类数据
        Map<String, String> categoryData = parseCategories(content);
        builder.categoryData(categoryData);
        builder.categories(new ArrayList<>(categoryData.keySet()));
    }

    /**
     * 从文本中提取字段值
     */
    private String extractField(String content, String fieldName, String... stopKeywords) {
        // 尝试多种格式匹配
        String[] patterns = {
            fieldName + "[：:]\\s*(.+?)(?=\\n|$)",
            fieldName + "\\s+(.+?)(?=\\n|$)",
            fieldName + "\\s*\\n(.+?)(?=\\n|$)"
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                String value = m.group(1).trim();
                // 检查是否需要停止
                for (String stop : stopKeywords) {
                    int stopIndex = value.indexOf(stop);
                    if (stopIndex > 0) {
                        value = value.substring(0, stopIndex).trim();
                    }
                }
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * 提取文本段落
     */
    private String extractSection(String content, String startKeyword, String... endKeywords) {
        int startIndex = content.indexOf(startKeyword);
        if (startIndex < 0) return null;

        startIndex += startKeyword.length();

        int endIndex = content.length();
        for (String endKeyword : endKeywords) {
            int idx = content.indexOf(endKeyword, startIndex);
            if (idx > 0 && idx < endIndex) {
                endIndex = idx;
            }
        }

        String section = content.substring(startIndex, endIndex).trim();
        // 去除开头的冒号或换行
        section = section.replaceFirst("^[：: \\n\\r\\t]+", "");
        return section.isEmpty() ? null : section;
    }

    /**
     * 解析十大股东信息
     */
    private List<ShareholderDTO> parseShareholders(String content) {
        List<ShareholderDTO> shareholders = new ArrayList<>();

        // 查找十大股东部分
        String shareholderSection = extractSection(content, "十大流通股东", "股东户数", "股东变化");
        if (shareholderSection == null) {
            shareholderSection = extractSection(content, "十大股东", "股东户数", "股东变化");
        }

        if (shareholderSection == null) {
            return shareholders;
        }

        // 解析股东列表（简化版，实际格式可能更复杂）
        String[] lines = shareholderSection.split("\n");
        int rank = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 尝试匹配股东行
            // 格式示例: 1 股东名称 1000000 5.00% 增加
            Pattern pattern = Pattern.compile("^(\\d+)\\s+(.+?)\\s+(\\d+)\\s+([\\d.]+)%?");
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                rank++;
                try {
                    ShareholderDTO shareholder = ShareholderDTO.builder()
                        .rank(rank)
                        .name(matcher.group(2).trim())
                        .shares(Long.parseLong(matcher.group(3)))
                        .ratio(new BigDecimal(matcher.group(4)))
                        .build();
                    shareholders.add(shareholder);
                } catch (Exception e) {
                    log.warn("解析股东行失败: {}", line);
                }
            }
        }

        return shareholders;
    }

    /**
     * 解析所有分类数据
     */
    private Map<String, String> parseCategories(String content) {
        Map<String, String> categories = new HashMap<>();

        // 常见的F10分类
        String[] commonCategories = {
            "公司概况", "公司简介",
            "主营业务", "经营范围",
            "股本结构", "股本变动",
            "股东研究", "十大股东", "十大流通股东",
            "财务分析", "主要财务指标",
            "分红融资", "分红配股",
            "公司高管", "高管人员",
            "公司公告", "重大事项"
        };

        for (String category : commonCategories) {
            if (content.contains(category)) {
                String section = extractSection(content, category,
                    "公司概况", "公司简介", "主营业务", "经营范围",
                    "股本结构", "股东研究", "财务分析", "分红融资", "公司高管");
                if (section != null && section.length() > 5) {
                    categories.put(category, section);
                }
            }
        }

        return categories;
    }

    /**
     * 解析数值（支持中文单位）
     */
    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim().replaceAll(",", "");

        // 处理中文单位
        if (value.contains("亿")) {
            value = value.replace("亿", "").trim();
            try {
                return new BigDecimal(value).multiply(new BigDecimal("100000000"));
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (value.contains("万")) {
            value = value.replace("万", "").trim();
            try {
                return new BigDecimal(value).multiply(new BigDecimal("10000"));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析日期
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDate.parse(value, DATE_FORMATTER2);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
