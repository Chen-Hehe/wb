package com.weibo.controller;

import com.weibo.common.Result;
import com.weibo.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报表控制器
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    
    private final ReportService reportService;
    
    /**
     * 动态报表查询
     * @param request 请求参数 {prompt: 自然语言需求，chartType: 图表类型}
     * @return 报表数据和 ECharts 配置
     */
    @PostMapping("/dynamic")
    public Result<?> dynamicReport(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String chartType = request.get("chartType");
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return Result.error("报表需求不能为空");
        }
        
        if (chartType == null || chartType.trim().isEmpty()) {
            chartType = "bar"; // 默认柱状图
        }
        
        try {
            log.info("收到动态报表请求：prompt={}, chartType={}", prompt, chartType);
            
            Map<String, Object> data = reportService.reportDynamic(prompt, chartType);
            
            return Result.success("查询成功", data);
        } catch (Exception e) {
            log.error("动态报表查询失败", e);
            return Result.error("报表生成失败：" + e.getMessage());
        }
    }
}
