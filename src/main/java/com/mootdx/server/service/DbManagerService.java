package com.mootdx.server.service;

import com.mootdx.server.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 数据库管理服务
 * 提供表管理、SQL执行、历史记录等功能
 */
@Slf4j
@Service
public class DbManagerService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    // SQL执行历史记录（内存存储）
    private final List<SqlHistoryItemDTO> sqlHistory = new CopyOnWriteArrayList<>();

    // 已知的表列表（用于检测新增表）
    private final Set<String> knownTables = Collections.synchronizedSet(new HashSet<>());

    // 表分类配置
    private static final Map<String, Map<String, String>> TABLE_CATEGORIES = new LinkedHashMap<>();

    static {
        // 股票基础信息
        Map<String, String> stockBasic = new HashMap<>();
        stockBasic.put("description", "股票基础信息相关表");
        stockBasic.put("keywords", "stock_basic");
        TABLE_CATEGORIES.put("股票基础", stockBasic);

        // 股票标签系统
        Map<String, String> stockTag = new HashMap<>();
        stockTag.put("description", "股票标签和分类系统");
        stockTag.put("keywords", "stock_tag,stock_tag_mapping,stock_tag_history,stock_technical_tag,stock_industry");
        TABLE_CATEGORIES.put("股票标签", stockTag);

        // 题材主题
        Map<String, String> stockTheme = new HashMap<>();
        stockTheme.put("description", "题材主题相关表");
        stockTheme.put("keywords", "stock_theme,stock_theme_detail,stock_theme_mapping");
        TABLE_CATEGORIES.put("题材主题", stockTheme);

        // K线数据
        Map<String, String> stockKline = new HashMap<>();
        stockKline.put("description", "K线行情数据");
        stockKline.put("keywords", "stock_kline");
        TABLE_CATEGORIES.put("K线数据", stockKline);

        // 持仓交易
        Map<String, String> trade = new HashMap<>();
        trade.put("description", "持仓和交易平台");
        trade.put("keywords", "stock_position,trade_platform");
        TABLE_CATEGORIES.put("持仓交易", trade);

        // 服务器管理
        Map<String, String> server = new HashMap<>();
        server.put("description", "TDX服务器配置和日志");
        server.put("keywords", "tdx_server,server_status,server_type");
        TABLE_CATEGORIES.put("服务器管理", server);

        // 定时任务
        Map<String, String> scheduler = new HashMap<>();
        scheduler.put("description", "定时任务和调度");
        scheduler.put("keywords", "scheduled_task,task_execution_log,data_collection_task");
        TABLE_CATEGORIES.put("定时任务", scheduler);
    }

    // SQL模板
    private static final List<SqlTemplateDTO> SQL_TEMPLATES = Arrays.asList(
            SqlTemplateDTO.builder()
                    .id("select_all").name("查询全部数据").category("查询")
                    .description("查询表中所有数据（带分页）")
                    .template("SELECT * FROM {table_name} LIMIT 100")
                    .icon("search").build(),
            SqlTemplateDTO.builder()
                    .id("select_count").name("统计行数").category("查询")
                    .description("统计表中的记录总数")
                    .template("SELECT COUNT(*) as total FROM {table_name}")
                    .icon("pie-chart").build(),
            SqlTemplateDTO.builder()
                    .id("select_where").name("条件查询").category("查询")
                    .description("根据条件查询数据")
                    .template("SELECT * FROM {table_name} WHERE 条件字段 = '值' LIMIT 100")
                    .icon("filter").build(),
            SqlTemplateDTO.builder()
                    .id("select_order").name("排序查询").category("查询")
                    .description("按指定字段排序查询")
                    .template("SELECT * FROM {table_name} ORDER BY id DESC LIMIT 100")
                    .icon("sort").build(),
            SqlTemplateDTO.builder()
                    .id("select_group").name("分组统计").category("查询")
                    .description("按字段分组统计")
                    .template("SELECT 分组字段, COUNT(*) as count FROM {table_name} GROUP BY 分组字段")
                    .icon("data-analysis").build(),
            SqlTemplateDTO.builder()
                    .id("select_join").name("关联查询").category("查询")
                    .description("多表关联查询")
                    .template("SELECT a.*, b.关联字段 \nFROM {table_name} a \nLEFT JOIN 关联表 b ON a.id = b.foreign_id \nLIMIT 100")
                    .icon("connection").build(),
            SqlTemplateDTO.builder()
                    .id("insert_single").name("插入单条").category("插入")
                    .description("插入单条记录")
                    .template("INSERT INTO {table_name} (字段1, 字段2, 字段3) \nVALUES ('值1', '值2', '值3')")
                    .icon("plus").build(),
            SqlTemplateDTO.builder()
                    .id("insert_select").name("插入查询结果").category("插入")
                    .description("将查询结果插入到表中")
                    .template("INSERT INTO 目标表 (字段1, 字段2)\nSELECT 字段1, 字段2 FROM {table_name} WHERE 条件")
                    .icon("document-copy").build(),
            SqlTemplateDTO.builder()
                    .id("update_basic").name("更新数据").category("更新")
                    .description("更新符合条件的记录")
                    .template("UPDATE {table_name} \nSET 字段1 = '新值1', 字段2 = '新值2' \nWHERE id = 1")
                    .icon("edit").build(),
            SqlTemplateDTO.builder()
                    .id("update_batch").name("批量更新").category("更新")
                    .description("批量更新多条记录")
                    .template("UPDATE {table_name} \nSET 状态字段 = '已完成' \nWHERE 条件字段 IN ('值1', '值2', '值3')")
                    .icon("edit-pen").build(),
            SqlTemplateDTO.builder()
                    .id("delete_single").name("删除单条").category("删除")
                    .description("删除指定ID的记录")
                    .template("DELETE FROM {table_name} WHERE id = 1")
                    .icon("delete").build(),
            SqlTemplateDTO.builder()
                    .id("delete_where").name("条件删除").category("删除")
                    .description("删除符合条件的记录")
                    .template("DELETE FROM {table_name} WHERE 条件字段 = '值'")
                    .icon("remove").build()
    );

    /**
     * 获取所有数据库表信息
     */
    public TableListResponseDTO getAllTables() {
        List<TableInfoDTO> tables = new ArrayList<>();
        List<String> newTables = new ArrayList<>();

        try (Connection connection = getConnection();
             ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");

                // 跳过H2系统表
                if (isSystemTable(tableName)) {
                    continue;
                }

                // 检测是否为新表
                boolean isNew = !knownTables.contains(tableName);
                if (isNew) {
                    newTables.add(tableName);
                }

                TableInfoDTO tableInfo = buildTableInfo(connection, tableName, isNew);
                tables.add(tableInfo);
            }

            // 更新已知表列表
            knownTables.clear();
            for (TableInfoDTO table : tables) {
                knownTables.add(table.getName());
            }

        } catch (SQLException e) {
            log.error("获取表列表失败", e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }

        // 构建分类列表
        List<TableCategoryDTO> categories = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : TABLE_CATEGORIES.entrySet()) {
            categories.add(TableCategoryDTO.builder()
                    .name(entry.getKey())
                    .description(entry.getValue().get("description"))
                    .build());
        }
        categories.add(TableCategoryDTO.builder().name("其他").description("未分类表").build());

        return TableListResponseDTO.builder()
                .tables(tables)
                .total(tables.size())
                .categories(categories)
                .hasNewTables(!newTables.isEmpty())
                .newTableCount(newTables.size())
                .build();
    }

    /**
     * 刷新并检测新增表
     */
    public TableListResponseDTO refreshTables() {
        // 保存当前已知表列表
        Set<String> previousTables = new HashSet<>(knownTables);

        // 重新获取所有表
        TableListResponseDTO response = getAllTables();

        // 检测新表（在当前列表中但不在之前的列表中）
        List<String> currentTables = new ArrayList<>();
        for (TableInfoDTO table : response.getTables()) {
            currentTables.add(table.getName());
        }

        List<String> newTables = new ArrayList<>();
        for (String table : currentTables) {
            if (!previousTables.contains(table)) {
                newTables.add(table);
            }
        }

        response.setHasNewTables(!newTables.isEmpty());
        response.setNewTableCount(newTables.size());

        if (!newTables.isEmpty()) {
            log.info("检测到新增表: {}", newTables);
        }

        return response;
    }

    /**
     * 获取新增表列表
     */
    public List<String> getNewTables() {
        List<String> currentTables = new ArrayList<>();
        List<String> newTables = new ArrayList<>();

        try (Connection connection = getConnection();
             ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (!isSystemTable(tableName)) {
                    currentTables.add(tableName);
                    if (!knownTables.contains(tableName)) {
                        newTables.add(tableName);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取新表列表失败", e);
        }

        return newTables;
    }

    /**
     * 构建表信息
     */
    private TableInfoDTO buildTableInfo(Connection connection, String tableName, boolean isNew) throws SQLException {
        // 获取列数
        int columnCount = 0;
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, "%")) {
            while (columns.next()) {
                columnCount++;
            }
        }

        // 获取行数
        Long rowCount = null;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
            if (rs.next()) {
                rowCount = rs.getLong(1);
            }
        } catch (SQLException e) {
            log.warn("获取表 {} 行数失败", tableName);
        }

        return TableInfoDTO.builder()
                .name(tableName)
                .comment(null)
                .rowCount(rowCount)
                .columnsCount(columnCount)
                .category(getTableCategory(tableName))
                .isNew(isNew)
                .build();
    }

    /**
     * 获取表分类
     */
    private TableCategoryDTO getTableCategory(String tableName) {
        String lowerTableName = tableName.toLowerCase();
        for (Map.Entry<String, Map<String, String>> entry : TABLE_CATEGORIES.entrySet()) {
            String keywords = entry.getValue().get("keywords");
            if (keywords != null) {
                for (String keyword : keywords.split(",")) {
                    if (lowerTableName.contains(keyword.trim())) {
                        return TableCategoryDTO.builder()
                                .name(entry.getKey())
                                .description(entry.getValue().get("description"))
                                .build();
                    }
                }
            }
        }
        return TableCategoryDTO.builder()
                .name("其他")
                .description("未分类表")
                .build();
    }

    /**
     * 判断是否系统表
     */
    private boolean isSystemTable(String tableName) {
        String upper = tableName.toUpperCase();
        return upper.startsWith("DATABASECHANGELOG") ||
               upper.startsWith("DATABASECHANGELOGLOCK");
    }

    /**
     * 获取表结构
     */
    public TableStructureDTO getTableStructure(String tableName) {
        try (Connection connection = getConnection()) {
            // 检查表是否存在
            boolean exists = false;
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
                exists = rs.next();
            }
            if (!exists) {
                throw new RuntimeException("表 " + tableName + " 不存在");
            }

            // 获取列信息
            List<ColumnInfoDTO> columns = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    columns.add(ColumnInfoDTO.builder()
                            .name(rs.getString("COLUMN_NAME"))
                            .type(rs.getString("TYPE_NAME"))
                            .nullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                            .defaultValue(rs.getString("COLUMN_DEF"))
                            .comment(rs.getString("REMARKS"))
                            .isPrimaryKey(false)
                            .build());
                }
            }

            // 获取主键
            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }

            // 更新列的主键标记
            for (ColumnInfoDTO column : columns) {
                if (primaryKeys.contains(column.getName())) {
                    column.setIsPrimaryKey(true);
                }
            }

            // 获取索引信息
            List<IndexInfoDTO> indexes = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
                Map<String, IndexInfoDTO> indexMap = new HashMap<>();
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName == null) continue;

                    IndexInfoDTO index = indexMap.get(indexName);
                    if (index == null) {
                        index = IndexInfoDTO.builder()
                                .name(indexName)
                                .columns(new ArrayList<>())
                                .unique(!rs.getBoolean("NON_UNIQUE"))
                                .build();
                        indexMap.put(indexName, index);
                    }
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName != null && !index.getColumns().contains(columnName)) {
                        index.getColumns().add(columnName);
                    }
                }
                indexes.addAll(indexMap.values());
            }

            return TableStructureDTO.builder()
                    .name(tableName)
                    .comment(null)
                    .columns(columns)
                    .indexes(indexes)
                    .primaryKeys(primaryKeys)
                    .build();

        } catch (SQLException e) {
            log.error("获取表结构失败: {}", tableName, e);
            throw new RuntimeException("获取表结构失败: " + e.getMessage());
        }
    }

    /**
     * 获取表数据
     */
    public TableDataResponseDTO getTableData(String tableName, int page, int pageSize,
                                             String sortColumn, String sortOrder) {
        try (Connection connection = getConnection()) {
            // 检查表是否存在
            boolean exists = false;
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
                exists = rs.next();
            }
            if (!exists) {
                throw new RuntimeException("表 " + tableName + " 不存在");
            }

            // 获取列信息
            List<Map<String, Object>> columnsDef = new ArrayList<>();
            List<String> columnNames = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    columnNames.add(colName);
                    Map<String, Object> colDef = new HashMap<>();
                    colDef.put("name", colName);
                    colDef.put("type", rs.getString("TYPE_NAME"));
                    colDef.put("comment", rs.getString("REMARKS") != null ? rs.getString("REMARKS") : colName);
                    columnsDef.add(colDef);
                }
            }

            // 获取总条数
            long total = 0;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
                if (rs.next()) {
                    total = rs.getLong(1);
                }
            }

            // 构建查询SQL
            int offset = (page - 1) * pageSize;
            String columnsStr = columnNames.stream()
                    .map(c -> "\"" + c + "\"")
                    .collect(Collectors.joining(", "));

            String orderClause = "";
            if (sortColumn != null && columnNames.contains(sortColumn)) {
                orderClause = " ORDER BY \"" + sortColumn + "\" " +
                        ("desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC");
            }

            String query = String.format("SELECT %s FROM \"%s\"%s LIMIT %d OFFSET %d",
                    columnsStr, tableName, orderClause, pageSize, offset);

            // 执行查询
            List<Map<String, Object>> data = new ArrayList<>();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int colCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(colName, value);
                    }
                    data.add(row);
                }
            }

            int totalPages = (int) Math.ceil((double) total / pageSize);

            return TableDataResponseDTO.builder()
                    .tableName(tableName)
                    .columns(columnsDef)
                    .data(data)
                    .total(total)
                    .page(page)
                    .pageSize(pageSize)
                    .totalPages(totalPages)
                    .build();

        } catch (SQLException e) {
            log.error("获取表数据失败: {}", tableName, e);
            throw new RuntimeException("获取表数据失败: " + e.getMessage());
        }
    }

    /**
     * 执行SQL
     */
    @Transactional
    public SqlExecuteResultDTO executeSql(String sql, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String sqlType = getSqlType(sql);

        // 验证SQL
        String validationError = validateSql(sql);
        if (validationError != null) {
            SqlExecuteResultDTO result = SqlExecuteResultDTO.builder()
                    .success(false)
                    .message("验证失败: " + validationError)
                    .executionTime(0.0)
                    .sqlType(sqlType)
                    .build();
            saveSqlHistory(sql, params, result, startTime);
            return result;
        }

        try {
            Query query = entityManager.createNativeQuery(sql);

            // 设置参数
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    try {
                        query.setParameter(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        // 参数不存在，忽略
                    }
                }
            }

            // 判断SQL类型
            boolean isSelect = sqlType.equals("SELECT");

            if (isSelect) {
                // 查询操作
                List<Object[]> resultList = query.getResultList();
                double executionTime = (System.currentTimeMillis() - startTime) / 1000.0;

                // 转换结果格式
                List<Map<String, Object>> dataList = new ArrayList<>();
                for (Object[] row : resultList) {
                    Map<String, Object> rowMap = new HashMap<>();
                    for (int i = 0; i < row.length; i++) {
                        rowMap.put("col" + i, row[i]);
                    }
                    dataList.add(rowMap);
                }

                SqlExecuteResultDTO result = SqlExecuteResultDTO.builder()
                        .success(true)
                        .message("查询成功，返回 " + resultList.size() + " 条记录")
                        .data(dataList)
                        .rowCount(resultList.size())
                        .executionTime(executionTime)
                        .sqlType(sqlType)
                        .build();
                saveSqlHistory(sql, params, result, startTime);
                return result;
            } else {
                // 更新操作
                int affectedRows = query.executeUpdate();
                double executionTime = (System.currentTimeMillis() - startTime) / 1000.0;

                SqlExecuteResultDTO result = SqlExecuteResultDTO.builder()
                        .success(true)
                        .message("执行成功，影响 " + affectedRows + " 行")
                        .rowCount(affectedRows)
                        .executionTime(executionTime)
                        .sqlType(sqlType)
                        .build();
                saveSqlHistory(sql, params, result, startTime);
                return result;
            }

        } catch (Exception e) {
            double executionTime = (System.currentTimeMillis() - startTime) / 1000.0;
            log.error("SQL执行失败: {}", sql, e);

            SqlExecuteResultDTO result = SqlExecuteResultDTO.builder()
                    .success(false)
                    .message("执行失败: " + e.getMessage())
                    .executionTime(executionTime)
                    .sqlType(sqlType)
                    .build();
            saveSqlHistory(sql, params, result, startTime);
            return result;
        }
    }

    /**
     * 批量执行SQL
     */
    @Transactional
    public List<SqlExecuteResultDTO> executeBatchSql(List<String> sqls) {
        List<SqlExecuteResultDTO> results = new ArrayList<>();
        for (String sql : sqls) {
            results.add(executeSql(sql, null));
        }
        return results;
    }

    /**
     * 获取SQL执行历史
     */
    public List<SqlHistoryItemDTO> getSqlHistory(int limit) {
        if (limit <= 0 || limit >= sqlHistory.size()) {
            return new ArrayList<>(sqlHistory);
        }
        return new ArrayList<>(sqlHistory.subList(sqlHistory.size() - limit, sqlHistory.size()));
    }

    /**
     * 清空SQL执行历史
     */
    public void clearSqlHistory() {
        sqlHistory.clear();
    }

    /**
     * 获取SQL模板
     */
    public List<SqlTemplateDTO> getSqlTemplates(String category) {
        if (category == null || category.isEmpty()) {
            return SQL_TEMPLATES;
        }
        return SQL_TEMPLATES.stream()
                .filter(t -> category.equals(t.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 获取SQL模板分类
     */
    public List<String> getSqlTemplateCategories() {
        return SQL_TEMPLATES.stream()
                .map(SqlTemplateDTO::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 导出表数据
     */
    public byte[] exportTableData(String tableName, String format) {
        TableDataResponseDTO data = getTableData(tableName, 1, 10000, null, null);

        if ("csv".equalsIgnoreCase(format)) {
            return exportToCsv(data);
        } else if ("json".equalsIgnoreCase(format)) {
            return exportToJson(data);
        } else if ("sql".equalsIgnoreCase(format)) {
            return exportToSql(tableName, data);
        }

        throw new RuntimeException("不支持的导出格式: " + format);
    }

    /**
     * 导出为CSV
     */
    private byte[] exportToCsv(TableDataResponseDTO data) {
        StringBuilder csv = new StringBuilder();

        // 表头
        List<String> headers = new ArrayList<>();
        for (Map<String, Object> col : data.getColumns()) {
            headers.add((String) col.get("name"));
        }
        csv.append(String.join(",", headers)).append("\n");

        // 数据
        for (Map<String, Object> row : data.getData()) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object value = row.get(header);
                values.add(value != null ? value.toString() : "");
            }
            csv.append(String.join(",", values)).append("\n");
        }

        return csv.toString().getBytes();
    }

    /**
     * 导出为JSON
     */
    private byte[] exportToJson(TableDataResponseDTO data) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        List<Map<String, Object>> rows = data.getData();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            json.append("  {");

            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String valueStr = value != null ? "\"" + value.toString().replace("\"", "\\\"") + "\"" : "null";
                entries.add("\"" + key + "\":" + valueStr);
            }
            json.append(String.join(",", entries));

            json.append("}");
            if (i < rows.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString().getBytes();
    }

    /**
     * 导出为SQL INSERT语句
     */
    private byte[] exportToSql(String tableName, TableDataResponseDTO data) {
        StringBuilder sql = new StringBuilder();

        List<String> columns = new ArrayList<>();
        for (Map<String, Object> col : data.getColumns()) {
            columns.add((String) col.get("name"));
        }

        for (Map<String, Object> row : data.getData()) {
            sql.append("INSERT INTO \"").append(tableName).append("\" (");
            sql.append(String.join(", ", columns));
            sql.append(") VALUES (");

            List<String> values = new ArrayList<>();
            for (String col : columns) {
                Object value = row.get(col);
                if (value == null) {
                    values.add("NULL");
                } else if (value instanceof Number) {
                    values.add(value.toString());
                } else {
                    values.add("'" + value.toString().replace("'", "''") + "'");
                }
            }
            sql.append(String.join(", ", values));
            sql.append(");\n");
        }

        return sql.toString().getBytes();
    }

    // ==================== 私有方法 ====================

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private String getSqlType(String sql) {
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) return "SELECT";
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        if (trimmed.startsWith("CREATE")) return "CREATE";
        if (trimmed.startsWith("DROP")) return "DROP";
        if (trimmed.startsWith("ALTER")) return "ALTER";
        return "OTHER";
    }

    private String validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "SQL不能为空";
        }

        String upper = sql.toUpperCase();

        // 禁止危险操作
        String[] forbidden = {"DROP DATABASE", "DROP TABLE", "TRUNCATE", "DELETE FROM", "UPDATE"};
        for (String f : forbidden) {
            if (upper.contains(f)) {
                // 允许DELETE和UPDATE但需要有WHERE
                if (f.equals("DELETE FROM") && !upper.contains("WHERE")) {
                    return "DELETE语句必须包含WHERE条件";
                }
                if (f.equals("UPDATE") && !upper.contains("WHERE")) {
                    return "UPDATE语句必须包含WHERE条件";
                }
            }
        }

        return null;
    }

    private void saveSqlHistory(String sql, Map<String, Object> params,
                                SqlExecuteResultDTO result, long startTime) {
        SqlHistoryItemDTO history = SqlHistoryItemDTO.builder()
                .id(UUID.randomUUID().toString())
                .sql(sql)
                .params(params)
                .success(result.getSuccess())
                .message(result.getMessage())
                .executionTime(result.getExecutionTime())
                .executedAt(LocalDateTime.now())
                .build();

        sqlHistory.add(history);

        // 限制历史记录数量
        if (sqlHistory.size() > 1000) {
            sqlHistory.remove(0);
        }
    }
}
