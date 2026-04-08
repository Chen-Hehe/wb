package com.weibo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weibo.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
 * AI 图像生成控制器
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private static final String DASHSCOPE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private final ObjectMapper objectMapper;

    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @RequestMapping(value = "/wan/txt2img", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<?> txt2img(@RequestParam("prompt") String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return Result.error("prompt 不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            return Result.error("未配置 DASHSCOPE_API_KEY");
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            Map<String, Object> body = Map.of(
                    "model", "wan2.7-image-pro",
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of(
                                            "role", "user",
                                            "content", List.of(Map.of("text", prompt))
                                    )
                            )
                    ),
                    "parameters", Map.of(
                            "size", "2K",
                            "n", 1,
                            "watermark", false,
                            "thinking_mode", true
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = root.path("message").asText();
                if (!StringUtils.hasText(message)) {
                    message = root.path("code").asText("AI 生成失败");
                }
                log.warn("万相生成失败 status={}, body={}", response.statusCode(), response.body());
                return Result.error("AI 生成失败：" + message);
            }

            String imageUrl = findFirstImageUrl(root);
            if (!StringUtils.hasText(imageUrl)) {
                log.warn("万相返回中未解析到图片地址: {}", response.body());
                return Result.error("AI 生成成功，但未获取到图片地址");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("prompt", prompt);
            data.put("imageUrl", imageUrl);
            data.put("requestId", root.path("request_id").asText());
            data.put("taskId", root.path("output").path("task_id").asText());
            return Result.success("AI 生成成功", data);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("调用万相文生图失败", e);
            return Result.error("调用 AI 生成图片失败：" + e.getMessage());
        }
    }

    private String findFirstImageUrl(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            JsonNode urlNode = node.get("url");
            if (urlNode != null && urlNode.isTextual()) {
                String url = urlNode.asText();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return url;
                }
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String result = findFirstImageUrl(entry.getValue());
                if (StringUtils.hasText(result)) {
                    return result;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                String result = findFirstImageUrl(item);
                if (StringUtils.hasText(result)) {
                    return result;
                }
            }
        }

        return null;
    }
}
