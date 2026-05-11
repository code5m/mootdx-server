package com.mootdx.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mootdx.server.dto.F10ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F10数据解析服务
 * 功能：解析F10数据并提供导出功能
 */
@Slf4j
@Service
public class F10ParserService {

    private final ObjectMapper objectMapper;

    public F10ParserService() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }

    /**
     * 解析F10数据
     * @param content F10文本内容
     * @return 解析结果
     */
    public F10ParseResult parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return F10ParseResult.builder()
                .success(false)
                .errorMessage("内容不能为空")
                .build();
        }

        // 创建解析结果
        F10ParseResult result = new F10ParseResult();
        result.setSuccess(true);
        
        // 解析元数据
        F10ParseResult.StockMetadata metadata = parseMetadata(content);
        result.setMetadata(metadata);
        
        // 解析财务指标
        List<F10ParseResult.FinancialIndicator> financialIndicators = parseFinancialIndicators(content);
        result.setFinancialIndicators(financialIndicators);
        
        // 解析EPS历史
        List<F10ParseResult.EpsHistory> epsHistory = parseEpsHistory(content);
        result.setEpsHistory(epsHistory);
        
        // 解析风险提示
        List<F10ParseResult.RiskAlert> riskAlerts = parseRiskAlerts(content);
        result.setRiskAlerts(riskAlerts);

        // 解析公司基本资料
        F10ParseResult.CompanyProfile companyProfile = parseCompanyProfile(content);
        result.setCompanyProfile(companyProfile);

        // 设置统计信息
        result.setRawTableCount(0);

        return result;
    }

    /**
     * 解析元数据
     */
    private F10ParseResult.StockMetadata parseMetadata(String content) {
        F10ParseResult.StockMetadata metadata = new F10ParseResult.StockMetadata();
        
        // 解析股票代码和名称
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            
            // 匹配 ◇000005 ST星源 格式
            if (line.startsWith("◇")) {
                String[] parts = line.substring(1).split("\\s+", 2);
                if (parts.length >= 1) {
                    metadata.setStockCode(parts[0].trim());
                }
                if (parts.length >= 2) {
                    metadata.setStockName(parts[1].trim());
                }
                metadata.setRawTitle(line);
            }
            
            // 匹配更新日期
            if (line.contains("更新日期：")) {
                String dateStr = line.substring(line.indexOf("更新日期：") + "更新日期：".length()).trim();
                // 处理格式如 2025-09-01◇
                dateStr = dateStr.replaceAll("[◇☆].*$", "").trim();
                try {
                    metadata.setUpdateDate(java.time.LocalDate.parse(dateStr));
                } catch (Exception e) {
                    log.warn("日期解析失败: {}", dateStr);
                }
            }
            
            // 匹配数据来源
            if (line.contains("港澳资讯") || line.contains("灵通V")) {
                metadata.setDataSource("港澳资讯 V8.0");
            }
        }
        
        // 默认值
        if (metadata.getDataSource() == null) {
            metadata.setDataSource("港澳资讯 V8.0");
        }
        
        return metadata;
    }

    /**
     * 解析财务指标
     */
    private List<F10ParseResult.FinancialIndicator> parseFinancialIndicators(String content) {
        List<F10ParseResult.FinancialIndicator> indicators = new ArrayList<>();
        
        String[] lines = content.split("\\r?\\n");
        boolean inTable = false;
        List<String> headers = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            
            // 检测最新主要指标表格
            if (line.contains("★最新主要指标★")) {
                inTable = true;
                continue;
            }
            
            // 检测表格结束
            if (inTable && line.startsWith("├───────────┴")) {
                break;
            }
            
            // 解析表头行
            if (inTable && line.startsWith("｜★最新主要指标★")) {
                headers = parseTableRow(line);
                continue;
            }
            
            // 解析数据行
            if (inTable && line.startsWith("｜") && !line.contains("★最新主要指标★")) {
                List<String> values = parseTableRow(line);
                if (values.size() >= 2) {
                    String indicatorName = values.get(0);
                    
                    // 为每个报告期创建指标
                    for (int i = 1; i < Math.min(values.size(), headers.size()); i++) {
                        String valueStr = values.get(i);
                        BigDecimal value = parseNumeric(valueStr);
                        
                        F10ParseResult.FinancialIndicator indicator = 
                            new F10ParseResult.FinancialIndicator();
                        indicator.setIndicatorName(indicatorName);
                        indicator.setValue(value);
                        indicator.setReportDate(parseReportDate(headers.get(i)));
                        indicator.setIsNegative(value != null && value.compareTo(BigDecimal.ZERO) < 0);
                        
                        // 提取单位
                        if (indicatorName.contains("(元)")) {
                            indicator.setUnit("元");
                        } else if (indicatorName.contains("(%)") || indicatorName.contains("(%)")) {
                            indicator.setUnit("%");
                        } else if (indicatorName.contains("(股)")) {
                            indicator.setUnit("股");
                        }
                        
                        indicators.add(indicator);
                    }
                }
            }
        }
        
        return indicators;
    }

    /**
     * 解析EPS历史
     */
    private List<F10ParseResult.EpsHistory> parseEpsHistory(String content) {
        List<F10ParseResult.EpsHistory> history = new ArrayList<>();
        
        String[] lines = content.split("\\r?\\n");
        boolean inEpsSection = false;
        List<String> headers = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            
            // 检测EPS对比表格开始
            if (line.contains("★近五年每股收益对比")) {
                inEpsSection = true;
                continue;
            }
            
            // 检测表格结束
            if (inEpsSection && line.startsWith("└")) {
                break;
            }
            
            // 解析表头
            if (inEpsSection && line.startsWith("｜年度") && headers.isEmpty()) {
                headers = parseTableRow(line);
                continue;
            }
            
            // 解析数据行
            if (inEpsSection && line.startsWith("｜") && !line.contains("年度")) {
                List<String> values = parseTableRow(line);
                if (values.size() >= 1) {
                    F10ParseResult.EpsHistory eps = new F10ParseResult.EpsHistory();
                    eps.setYear(values.get(0));
                    
                    // 解析各期数据
                    if (values.size() > 1) {
                        eps.setAnnualEps(parseNumeric(values.get(1)));
                    }
                    if (values.size() > 2) {
                        eps.setQ3Eps(parseNumeric(values.get(2)));
                    }
                    if (values.size() > 3) {
                        eps.setHalfYearEps(parseNumeric(values.get(3)));
                    }
                    if (values.size() > 4) {
                        eps.setQ1Eps(parseNumeric(values.get(4)));
                    }
                    
                    history.add(eps);
                }
            }
        }
        
        return history;
    }

    /**
     * 解析风险提示
     */
    private List<F10ParseResult.RiskAlert> parseRiskAlerts(String content) {
        List<F10ParseResult.RiskAlert> alerts = new ArrayList<>();
        
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            
            // 退市风险
            if (line.contains("退市") || line.contains("终止上市")) {
                F10ParseResult.RiskAlert alert = new F10ParseResult.RiskAlert();
                alert.setRiskType("退市风险");
                alert.setTitle("退市");
                alert.setContent(line);
                alert.setRiskLevel("极高");
                
                // 尝试提取日期
                Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        alert.setAlertDate(java.time.LocalDate.parse(matcher.group(1)));
                    } catch (Exception e) {
                        log.warn("日期解析失败");
                    }
                }
                
                alerts.add(alert);
            }
            
            // 股权质押风险
            if (line.contains("质押")) {
                F10ParseResult.RiskAlert alert = new F10ParseResult.RiskAlert();
                alert.setRiskType("股权质押");
                alert.setTitle("股权质押比例过高");
                alert.setContent(line);
                alert.setRiskLevel("高");
                
                // 尝试提取质押比例
                Pattern pattern = Pattern.compile("(\\d+\\.\\d+)%");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        alert.setMarkValue(Double.parseDouble(matcher.group(1)) / 100);
                    } catch (Exception e) {
                        log.warn("比例解析失败");
                    }
                }
                
                alerts.add(alert);
            }
            
            // 亏损风险（L标记）
            if (line.contains("(L)") || line.contains("亏损")) {
                F10ParseResult.RiskAlert alert = new F10ParseResult.RiskAlert();
                alert.setRiskType("业绩亏损");
                alert.setTitle("持续亏损");
                alert.setContent(line);
                alert.setRiskLevel("高");
                alerts.add(alert);
            }
        }
        
        return alerts;
    }

    /**
     * 解析公司基本资料
     */
    private F10ParseResult.CompanyProfile parseCompanyProfile(String content) {
        F10ParseResult.CompanyProfile profile = new F10ParseResult.CompanyProfile();
        Map<String, String> additionalInfo = new HashMap<>();

        String[] lines = content.split("\\r?\\n");
        boolean inCompanyIntroSection = false;
        StringBuilder companyIntroBuilder = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 检测公司简介部分开始
            if (line.contains("【公司简介】") || line.contains("★公司简介") || line.contains("公司概况")) {
                inCompanyIntroSection = true;
                continue;
            }
            
            // 检测公司简介部分结束（遇到下一个标题或表格）
            if (inCompanyIntroSection && (line.startsWith("【") || line.startsWith("┌") || line.startsWith("★"))) {
                inCompanyIntroSection = false;
            }
            
            // 收集公司简介内容
            if (inCompanyIntroSection && !line.isEmpty()) {
                // 移除框线字符
                String cleanLine = line.replaceAll("[├┤┌┬┐└┴┘─│｜]", "").trim();
                if (!cleanLine.isEmpty()) {
                    if (companyIntroBuilder.length() > 0) {
                        companyIntroBuilder.append("\n");
                    }
                    companyIntroBuilder.append(cleanLine);
                }
            }
            
            // 解析主营业务
            if (line.contains("★主营业务:")) {
                String business = line.substring(line.indexOf("★主营业务:") + "★主营业务:".length()).trim();
                // 处理跨行 - 检查下一行是否以｜开头但不是表格线
                if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    if (nextLine.startsWith("｜") && !nextLine.contains("├") && !nextLine.contains("┌")) {
                        String nextContent = nextLine.replaceAll("[├┤┌┬┐└┴┘─│｜]", "").trim();
                        if (!nextContent.isEmpty()) {
                            business += nextContent;
                        }
                    }
                }
                profile.setBusinessScope(business);
            }
            
            // 解析其他键值对
            if (line.startsWith("｜") || line.startsWith("│")) {
                parseProfileLine(line, profile, additionalInfo);
            }
        }
        
        // 设置公司简介
        if (companyIntroBuilder.length() > 0) {
            profile.setCompanyIntroduction(companyIntroBuilder.toString());
        }

        profile.setAdditionalInfo(additionalInfo);
        return profile;
    }

    /**
     * 解析基本资料行
     */
    private void parseProfileLine(String line, F10ParseResult.CompanyProfile profile, Map<String, String> additionalInfo) {
        // 移除框线字符，提取内容
        String content = line.replaceAll("[┌┬┐├┼┤└┴┘─]", "").trim();

        // 按竖线分割
        String[] parts = content.split("[｜│]");

        // 过滤空值
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }

        // 成对处理字段和值
        // 支持两种格式：
        // 1. 字段1、值1、字段2、值2（深振业A格式）
        // 2. 字段1、值1（单行单字段格式）
        for (int i = 0; i < values.size() - 1; i += 2) {
            String field = values.get(i);
            String value = values.get(i + 1);
            
            // 如果字段看起来像值（包含数字或特殊字符），可能是格式不对，跳过
            if (isLikelyValue(field) && i > 0) {
                continue;
            }
            
            mapProfileField(field, value, profile, additionalInfo);
        }
    }
    
    /**
     * 判断字符串是否可能是值而不是字段名
     */
    private boolean isLikelyValue(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // 字段名通常是2-6个汉字，或者以字母开头的英文
        // 如果包含数字、小数点、百分号，或者是纯数字，可能是数值
        // 但公司名称、地址等可能包含括号，不能简单判断
        return str.matches("^[0-9].*") ||  // 以数字开头
               str.matches(".*[0-9\\\\.%].*") ||  // 包含数字、小数点、百分号
               str.matches("^\\d{4}-\\d{2}-\\d{2}$");  // 日期格式
    }

    /**
     * 映射基本资料字段
     */
    private void mapProfileField(String field, String value, F10ParseResult.CompanyProfile profile, Map<String, String> additionalInfo) {
        if (value == null || value.equals("-") || value.isEmpty()) {
            return;
        }

        switch (field) {
            case "公司名称":
                profile.setCompanyName(value);
                break;
            case "英文名称":
                profile.setEnglishName(value);
                break;
            case "证券简称":
                profile.setStockName(value);
                break;
            case "证券代码":
                profile.setStockCode(value);
                break;
            case "曾用简称":
            case "曾用简称 ":
                profile.setFormerName(value);
                break;
            case "关联上市":
                profile.setRelatedIndex(value);
                break;
            case "相关指数":
            case "相关指数 ":
                profile.setRelatedIndex(value);
                break;
            case "行业类别":
                profile.setIndustryCategory(value);
                break;
            case "证券类别":
                profile.setSecurityType(value);
                break;
            case "上市日期":
                try {
                    profile.setListingDate(java.time.LocalDate.parse(value));
                } catch (Exception e) {
                    additionalInfo.put(field, value);
                }
                break;
            case "成立日期":
                try {
                    profile.setEstablishmentDate(java.time.LocalDate.parse(value));
                } catch (Exception e) {
                    additionalInfo.put(field, value);
                }
                break;
            case "注册资本":
                profile.setRegisteredCapital(value);
                break;
            case "社会信用代码":
                profile.setCreditCode(value);
                break;
            case "法人代表":
                profile.setLegalRepresentative(value);
                break;
            case "总 裁":
                profile.setPresident(value);
                break;
            case "公司董秘":
                profile.setSecretary(value);
                break;
            case "证券事务代表":
                profile.setSecuritiesRep(value);
                break;
            case "联系电话":
                profile.setPhone(value);
                break;
            case "传 真":
                profile.setFax(value);
                break;
            case "公司网址":
                profile.setWebsite(value);
                break;
            case "电子信箱":
                profile.setEmail(value);
                break;
            case "注册地址":
                profile.setRegisteredAddress(value);
                break;
            case "办公地址":
                profile.setOfficeAddress(value);
                break;
            case "会计事务所":
                profile.setAccountingFirm(value);
                break;
            default:
                additionalInfo.put(field, value);
                break;
        }
    }

    /**
     * 解析表格行
     */
    private List<String> parseTableRow(String line) {
        List<String> cells = new ArrayList<>();
        // 按 ｜ 或 │ 分割
        String[] parts = line.split("[｜│]");
        for (String part : parts) {
            String cell = part.trim();
            if (!cell.isEmpty() && !cell.matches("[┌┬┐├┼┤└┴┘─]+")) {
                cells.add(cell);
            }
        }
        return cells;
    }

    /**
     * 解析报告日期
     */
    private java.time.LocalDate parseReportDate(String dateStr) {
        try {
            return java.time.LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 数值解析（处理 亿、万、%、- 等）
     */
    private BigDecimal parseNumeric(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return null;
        }
        
        String cleanValue = value.trim();
        
        // 处理单位
        if (cleanValue.contains("亿")) {
            cleanValue = cleanValue.replace("亿", "").trim();
            try {
                return new BigDecimal(cleanValue).multiply(new BigDecimal("100000000"));
            } catch (Exception e) {
                return null;
            }
        } else if (cleanValue.contains("万")) {
            cleanValue = cleanValue.replace("万", "").trim();
            try {
                return new BigDecimal(cleanValue).multiply(new BigDecimal("10000"));
            } catch (Exception e) {
                return null;
            }
        } else if (cleanValue.contains("%")) {
            cleanValue = cleanValue.replace("%", "").trim();
            try {
                return new BigDecimal(cleanValue).divide(new BigDecimal("100"));
            } catch (Exception e) {
                return null;
            }
        }
        
        // 处理标记（P=扭亏为盈，L=持续亏损）
        cleanValue = cleanValue.replace("(P)", "").replace("(L)", "").trim();
        
        try {
            return new BigDecimal(cleanValue);
        } catch (Exception e) {
            return null;
        }
    }
}
