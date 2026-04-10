package com.weibo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weibo.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;
    
    private static final String DASHSCOPE_CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    /**
     * 数据库表结构说明（用于 AI 生成 SQL）
     */
    private static final String DB_SCHEMA = """
        数据库表结构：
        1. attentions 表：att_id(主键), att_userid(关注者 ID), att_marstid(被关注者 ID)
        2. comments 表：cm_id(主键), cm_weiboid(微博 ID), cm_userid(用户 ID), cm_content(评论内容), cm_createtime(评论时间)
        3. users 表：user_id(主键), user_nickname(昵称), user_loginname(登录名), user_loginpwd(密码，禁止查询), user_photo(头像), user_score(积分), user_attionCount(关注数), user_email(邮箱)
        4. weibos 表：wb_id(主键), wb_userid(用户 ID), wb_title(标题), wb_content(内容), wb_createtime(创建时间), wb_readcount(阅读量), wb_img(图片), wb_pass(审核状态), wb_remark(审核备注)
        
        重要规则：
        1. 只能生成 SELECT 查询语句，禁止 DELETE/UPDATE/INSERT/DROP/ALTER
        2. 禁止查询 user_loginpwd 字段，如果涉及必须返回 "***"
        3. 查询结果字段统一别名为 name 和 value
        4. 只输出 SQL，不要有任何解释文字
        """;
    
    @Override
    public Map<String, Object> reportDynamic(String prompt, String chartType) {
        log.info("收到报表需求：prompt={}, chartType={}", prompt, chartType);
        
        // 1. 调用 AI 生成 SQL
        String sql = generateSqlByAi(prompt);
        log.info("AI 生成的 SQL: {}", sql);
        
        // 2. 安全校验 SQL
        if (!isValidSql(sql)) {
            throw new RuntimeException("SQL 校验失败：包含不安全语句");
        }
        
        // 3. 执行 SQL 查询
        List<Map<String, Object>> reportData = executeSql(sql);
        log.info("查询结果条数：{}", reportData.size());
        
        // 4. 生成 ECharts 配置
        String echartOption = createEchartOption(reportData, chartType, prompt);
        log.info("生成的 ECharts 配置：{}", echartOption);
        
        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("report_data", reportData);
        result.put("echart_option", echartOption);
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> executeSql(String sql) {
        // 脱敏处理：如果查询包含 user_loginpwd，替换为 "***"
        String safeSql = sql.replace("user_loginpwd", "'***' AS user_loginpwd");
        
        try {
            return jdbcTemplate.queryForList(safeSql);
        } catch (Exception e) {
            log.error("执行 SQL 失败：{}", sql, e);
            throw new RuntimeException("查询执行失败：" + e.getMessage());
        }
    }
    
    @Override
    public String createEchartOption(List<Map<String, Object>> data, String chartType, String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            String dataJson = objectMapper.writeValueAsString(data);
            
            String promptText = String.format("""
                请根据以下数据和图表类型生成 ECharts 配置 option 对象。
                
                数据：%s
                图表类型：%s
                报表主题：%s
                
                要求：
                1. 只输出 JavaScript 代码，格式为：let option = {...};
                2. 不要有任何解释文字
                3. 图表标题使用报表主题
                4. 确保 option 是完整的可执行配置
                5. 如果是饼图，使用 data 中的 name 和 value 字段
                6. 如果是柱状图或折线图，name 作为 x 轴，value 作为 y 轴
                """, dataJson, chartType, prompt);
            
            Map<String, Object> body = Map.of(
                "model", "qwen-plus",
                "messages", List.of(
                    Map.of("role", "system", "content", "你是一个 ECharts 图表配置生成器，只输出 JavaScript 代码。"),
                    Map.of("role", "user", "content", promptText)
                ),
                "temperature", 0.1
            );
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_CHAT_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            
            JsonNode root = objectMapper.readTree(responseBody);
            String aiResponse = root.path("choices").path(0).path("message").path("content").asText();
            
            // 提取 option 代码
            String optionCode = extractOptionCode(aiResponse);
            
            return optionCode != null ? optionCode : "let option = {title: {text: '图表配置生成失败'}};";
            
        } catch (Exception e) {
            log.error("生成 ECharts 配置失败", e);
            return "let option = {title: {text: '图表配置生成失败: " + e.getMessage() + "'}};";
        }
    }
    
    /**
     * 调用 AI 生成 SQL
     */
    private String generateSqlByAi(String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            Map<String, Object> body = Map.of(
                "model", "qwen-plus",
                "messages", List.of(
                    Map.of("role", "system", "content", DB_SCHEMA),
                    Map.of("role", "user", "content", "请根据以下需求生成 SQL 查询语句：" + prompt)
                ),
                "temperature", 0.1
            );
            
            String jsonBody = objectMapper.writeValueAsString(body);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_CHAT_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            
            JsonNode root = objectMapper.readTree(responseBody);
            String sql = root.path("choices").path(0).path("message").path("content").asText().trim();
            
            // 清理 SQL（去掉可能的 markdown 标记）
            sql = sql.replace("```sql", "").replace("```", "").trim();
            
            return sql;
            
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("调用 AI 生成 SQL 失败", e);
            throw new RuntimeException("SQL 生成失败：" + e.getMessage());
        }
    }
    
    /**
     * SQL 安全校验
     */
    private boolean isValidSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 只允许 SELECT 语句
        if (!upperSql.startsWith("SELECT")) {
            return false;
        }
        
        // 禁止危险关键字
        String[] dangerousKeywords = {"DELETE", "UPDATE", "INSERT", "DROP", "ALTER", "TRUNCATE", "EXEC", "EXECUTE"};
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 从 AI 响应中提取 option 代码
     */
    private String extractOptionCode(String text) {
        if (text == null) return null;
        
        // 尝试查找 let option = ...
        int start = text.indexOf("let option");
        if (start >= 0) {
            int end = text.lastIndexOf("};");
            if (end > start) {
                return text.substring(start, end + 2).trim();
            }
        }
        
        // 尝试查找 const option = ...
        start = text.indexOf("const option");
        if (start >= 0) {
            int end = text.lastIndexOf("};");
            if (end > start) {
                return text.substring(start, end + 2).trim();
            }
        }
        
        // 尝试查找 { ... }
        int braceStart = text.indexOf("{");
        int braceEnd = text.lastIndexOf("}");
        if (braceStart >= 0 && braceEnd > braceStart) {
            return "let option = " + text.substring(braceStart, braceEnd + 1) + ";";
        }
        
        return null;
    }
}
