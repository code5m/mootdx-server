package com.mootdx.server.service;

import com.mootdx.server.entity.*;
import com.mootdx.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 股票标签服务
 * 支持多种标签类型的统一管理
 */
@Service
public class StockTagService {

    @Autowired
    private StockTagRepository stockTagRepository;

    @Autowired
    private StockTagMappingRepository stockTagMappingRepository;

    @Autowired
    private StockTechnicalTagRepository stockTechnicalTagRepository;

    @Autowired
    private StockIndustryRepository stockIndustryRepository;

    @Autowired
    private StockTagHistoryRepository stockTagHistoryRepository;

    // ==================== 标签定义管理 ====================

    @Transactional
    public StockTag createTag(String tagId, String tagName, String tagType, Map<String, Object> params) {
        Optional<StockTag> existing = stockTagRepository.findByTagId(tagId);
        if (existing.isPresent()) {
            return existing.get();
        }

        StockTag tag = new StockTag();
        tag.setTagId(tagId);
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        tag.setTagLevel(params.containsKey("tagLevel") ? (Integer) params.get("tagLevel") : 1);
        tag.setParentTagId((String) params.get("parentTagId"));
        tag.setKeywords((String) params.get("keywords"));
        tag.setDescription((String) params.get("description"));
        tag.setColor((String) params.get("color"));
        tag.setIcon((String) params.get("icon"));
        tag.setSource((String) params.get("source"));
        tag.setSourceCode((String) params.get("sourceCode"));
        tag.setStockCount(0);

        return stockTagRepository.save(tag);
    }

    @Transactional
    public StockTag updateTag(String tagId, Map<String, Object> params) {
        StockTag tag = stockTagRepository.findByTagId(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId));

        if (params.containsKey("tagName")) tag.setTagName((String) params.get("tagName"));
        if (params.containsKey("tagType")) tag.setTagType((String) params.get("tagType"));
        if (params.containsKey("tagLevel")) tag.setTagLevel((Integer) params.get("tagLevel"));
        if (params.containsKey("parentTagId")) tag.setParentTagId((String) params.get("parentTagId"));
        if (params.containsKey("keywords")) tag.setKeywords((String) params.get("keywords"));
        if (params.containsKey("description")) tag.setDescription((String) params.get("description"));
        if (params.containsKey("color")) tag.setColor((String) params.get("color"));
        if (params.containsKey("icon")) tag.setIcon((String) params.get("icon"));

        return stockTagRepository.save(tag);
    }

    public Optional<StockTag> getTagById(String tagId) {
        return stockTagRepository.findByTagId(tagId);
    }

    public List<StockTag> getTagsByType(String tagType) {
        return stockTagRepository.findByTagTypeOrderByStockCountDesc(tagType);
    }

    public List<StockTag> getTagsByTypeAndParent(String tagType, String parentTagId) {
        return stockTagRepository.findByTagTypeAndParentTagId(tagType, parentTagId);
    }

    public List<StockTag> getAllTags() {
        return stockTagRepository.findAll();
    }

    public List<StockTag> searchTags(String keyword, String tagType) {
        if (tagType != null && !tagType.isEmpty()) {
            return stockTagRepository.searchByKeywordAndType(keyword, tagType);
        }
        return stockTagRepository.searchByKeyword(keyword);
    }

    @Transactional
    public void deleteTag(String tagId) {
        stockTagMappingRepository.deleteAll(stockTagMappingRepository.findByTagId(tagId));
        stockTagRepository.findByTagId(tagId).ifPresent(stockTagRepository::delete);
    }

    // ==================== 股票-标签关联管理 ====================

    @Transactional
    public StockTagMapping addTagToStock(String code, String market, String tagId, Integer weight, String operator) {
        StockTag tag = stockTagRepository.findByTagId(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId));

        Optional<StockTagMapping> existing = stockTagMappingRepository.findByCodeAndMarketAndTagId(code, market, tagId);
        if (existing.isPresent()) {
            return existing.get();
        }

        StockTagMapping mapping = new StockTagMapping();
        mapping.setCode(code);
        mapping.setMarket(market.toUpperCase());
        mapping.setTagId(tagId);
        mapping.setTagType(tag.getTagType());
        mapping.setWeight(weight != null ? weight : 0);

        StockTagMapping saved = stockTagMappingRepository.save(mapping);

        // 记录历史
        recordHistory(code, market, tagId, tag.getTagType(), "ADD", null, null, operator);

        // 更新标签统计
        updateTagStats(tagId);

        return saved;
    }

    @Transactional
    public boolean removeTagFromStock(String code, String market, String tagId, String operator) {
        Optional<StockTagMapping> mapping = stockTagMappingRepository.findByCodeAndMarketAndTagId(code, market, tagId);
        if (mapping.isPresent()) {
            stockTagMappingRepository.delete(mapping.get());

            StockTag tag = stockTagRepository.findByTagId(tagId).orElse(null);
            if (tag != null) {
                recordHistory(code, market, tagId, tag.getTagType(), "REMOVE", null, null, operator);
                updateTagStats(tagId);
            }
            return true;
        }
        return false;
    }

    public List<Map<String, Object>> getStockTags(String code, String market, String tagType) {
        List<StockTagMapping> mappings;
        if (tagType != null && !tagType.isEmpty()) {
            mappings = stockTagMappingRepository.findByCodeAndMarketAndTagType(code, market, tagType);
        } else {
            mappings = stockTagMappingRepository.findByCodeAndMarket(code, market);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StockTagMapping mapping : mappings) {
            Optional<StockTag> tagOpt = stockTagRepository.findByTagId(mapping.getTagId());
            if (tagOpt.isPresent()) {
                StockTag tag = tagOpt.get();
                Map<String, Object> item = new HashMap<>();
                item.put("tagId", tag.getTagId());
                item.put("tagName", tag.getTagName());
                item.put("tagType", tag.getTagType());
                item.put("color", tag.getColor());
                item.put("weight", mapping.getWeight());
                item.put("createdAt", mapping.getCreatedAt());
                result.add(item);
            }
        }
        return result;
    }

    public List<Map<String, Object>> getStocksByTag(String tagId, Integer limit) {
        List<StockTagMapping> mappings = stockTagMappingRepository.findByTagIdOrderByWeightDesc(tagId);
        if (limit != null && limit > 0) {
            mappings = mappings.stream().limit(limit).collect(Collectors.toList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StockTagMapping mapping : mappings) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", mapping.getCode());
            item.put("market", mapping.getMarket());
            item.put("weight", mapping.getWeight());
            result.add(item);
        }
        return result;
    }

    // ==================== 技术指标标签管理 ====================

    @Transactional
    public StockTechnicalTag addTechnicalTag(String code, String market, String indicatorName,
                                              LocalDate tradeDate, String signalType, Map<String, Object> params) {
        Optional<StockTechnicalTag> existing = stockTechnicalTagRepository
                .findByCodeAndMarketAndIndicatorNameAndTradeDateAndSignalType(code, market, indicatorName, tradeDate, signalType);

        if (existing.isPresent()) {
            return existing.get();
        }

        StockTechnicalTag techTag = new StockTechnicalTag();
        techTag.setCode(code);
        techTag.setMarket(market.toUpperCase());
        techTag.setIndicatorName(indicatorName);
        techTag.setSignalType(signalType);
        techTag.setTradeDate(tradeDate);

        if (params != null) {
            techTag.setTradeTime(params.containsKey("tradeTime") ? (LocalDateTime) params.get("tradeTime") : null);
            techTag.setFreq(params.containsKey("freq") ? (String) params.get("freq") : "day");
            techTag.setIndicatorValue(params.containsKey("indicatorValue") ? ((Number) params.get("indicatorValue")).doubleValue() : null);
            techTag.setSignalStrength(params.containsKey("signalStrength") ? (Integer) params.get("signalStrength") : 3);
            techTag.setTriggerPrice(params.containsKey("triggerPrice") ? ((Number) params.get("triggerPrice")).doubleValue() : null);
            techTag.setTriggerVolume(params.containsKey("triggerVolume") ? (Integer) params.get("triggerVolume") : null);
            techTag.setDescription((String) params.get("description"));
            techTag.setIsActive(params.containsKey("isActive") ? (Integer) params.get("isActive") : 1);
            techTag.setConfirmed(params.containsKey("confirmed") ? (Integer) params.get("confirmed") : 0);
        }

        return stockTechnicalTagRepository.save(techTag);
    }

    public List<StockTechnicalTag> getTechnicalTags(String code, String market, String indicatorName,
                                                     String signalType, LocalDate tradeDate,
                                                     LocalDate startDate, LocalDate endDate,
                                                     Integer isActive, Integer limit) {
        List<StockTechnicalTag> results = stockTechnicalTagRepository.searchTechnicalTags(
                code, market, indicatorName, signalType, tradeDate, startDate, endDate, isActive);

        if (limit != null && limit > 0) {
            return results.stream().limit(limit).collect(Collectors.toList());
        }
        return results;
    }

    public List<Map<String, Object>> getStocksByTechnicalSignal(String indicatorName, String signalType,
                                                                 LocalDate tradeDate, Integer minStrength) {
        List<StockTechnicalTag> tags = stockTechnicalTagRepository.findBySignal(
                indicatorName, signalType, tradeDate, minStrength != null ? minStrength : 1);

        List<Map<String, Object>> result = new ArrayList<>();
        for (StockTechnicalTag tag : tags) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", tag.getCode());
            item.put("market", tag.getMarket());
            item.put("indicatorValue", tag.getIndicatorValue());
            item.put("signalStrength", tag.getSignalStrength());
            item.put("triggerPrice", tag.getTriggerPrice());
            item.put("description", tag.getDescription());
            result.add(item);
        }
        return result;
    }

    // ==================== 行业分类管理 ====================

    @Transactional
    public StockIndustry setStockIndustry(String code, String market, String standard,
                                           String level1Code, String level1Name, Map<String, Object> params) {
        Optional<StockIndustry> existing = stockIndustryRepository.findByCodeAndMarketAndStandard(code, market, standard);

        StockIndustry industry;
        if (existing.isPresent()) {
            industry = existing.get();
        } else {
            industry = new StockIndustry();
            industry.setCode(code);
            industry.setMarket(market.toUpperCase());
            industry.setStandard(standard);
        }

        industry.setIndustryLevel1Code(level1Code);
        industry.setIndustryLevel1Name(level1Name);

        if (params != null) {
            industry.setIndustryLevel2Code((String) params.get("level2Code"));
            industry.setIndustryLevel2Name((String) params.get("level2Name"));
            industry.setIndustryLevel3Code((String) params.get("level3Code"));
            industry.setIndustryLevel3Name((String) params.get("level3Name"));
            industry.setEffectiveDate(params.containsKey("effectiveDate") ? (LocalDate) params.get("effectiveDate") : LocalDate.now());
        }

        return stockIndustryRepository.save(industry);
    }

    public Optional<StockIndustry> getStockIndustry(String code, String market, String standard) {
        return stockIndustryRepository.findByCodeAndMarketAndStandard(code, market, standard);
    }

    public List<StockIndustry> getStockIndustries(String code, String market) {
        return stockIndustryRepository.findByCodeAndMarket(code, market);
    }

    // ==================== 标签统计 ====================

    @Transactional
    public void updateTagStats(String tagId) {
        long count = stockTagMappingRepository.countByTagId(tagId);
        stockTagRepository.findByTagId(tagId).ifPresent(tag -> {
            tag.setStockCount((int) count);
            stockTagRepository.save(tag);
        });
    }

    public Map<String, Long> getTagStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", stockTagRepository.count());
        stats.put("theme", stockTagRepository.findByTagType("theme").stream().count());
        stats.put("industry", stockTagRepository.findByTagType("industry").stream().count());
        stats.put("concept", stockTagRepository.findByTagType("concept").stream().count());
        stats.put("technical", stockTagRepository.findByTagType("technical").stream().count());
        stats.put("region", stockTagRepository.findByTagType("region").stream().count());
        stats.put("style", stockTagRepository.findByTagType("style").stream().count());
        return stats;
    }

    // ==================== 历史记录 ====================

    @Transactional
    public void recordHistory(String code, String market, String tagId, String tagType,
                              String action, String oldValue, String newValue, String operator) {
        StockTagHistory history = new StockTagHistory();
        history.setCode(code);
        history.setMarket(market.toUpperCase());
        history.setTagId(tagId);
        history.setTagType(tagType);
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setOperator(operator);
        history.setSource("api");
        stockTagHistoryRepository.save(history);
    }

    public List<StockTagHistory> getStockTagHistory(String code, String market) {
        return stockTagHistoryRepository.findByCodeAndMarketOrderByCreatedAtDesc(code, market);
    }
}
