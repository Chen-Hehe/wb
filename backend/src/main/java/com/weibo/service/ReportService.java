package com.weibo.service;

import java.util.List;
import java.util.Map;

/**
 * 报表服务接口
 */
public interface ReportService {
    
    /**
     * 动态报表查询
     * @param prompt 自然语言需求
     * @param chartType 图表类型
     * @return 报表数据和 ECharts 配置
     */
    Map<String, Object> reportDynamic(String prompt, String chartType);
    
    /**
     * 根据 SQL 执行查询
     * @param sql SQL 语句
     * @return 查询结果
     */
    List<Map<String, Object>> executeSql(String sql);
    
    /**
     * 生成 ECharts 配置
     * @param data 报表数据
     * @param chartType 图表类型
     * @param prompt 原始需求（用于生成标题）
     * @return ECharts option 字符串
     */
    String createEchartOption(List<Map<String, Object>> data, String chartType, String prompt);
}
