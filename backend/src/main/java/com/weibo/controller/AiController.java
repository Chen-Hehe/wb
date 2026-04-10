package com.weibo.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
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
    private static final String DASHSCOPE_CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String DASHSCOPE_ASYNC_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image-generation/generation";
    private static final String DASHSCOPE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/";

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
            String responseBody = response.body();
            log.info("===== 阿里云 API 响应 =====");
            log.info("状态码：{}", response.statusCode());
            log.info("响应体：{}", responseBody);
            log.info("========================");

            JsonNode root = objectMapper.readTree(responseBody);

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

    /**
     * 异步文生图 - 提交任务
     */
    @RequestMapping(value = "/wan/txt2imgAsync", method = {RequestMethod.POST})
    public Result<?> txt2imgAsync(@RequestBody Map<String, String> body) {
        String prompt = body != null ? body.get("prompt") : null;
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

            // 异步请求体格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "wan2.7-image-pro");
            
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> promptObj = new HashMap<>();
            promptObj.put("prompt", prompt);
            input.put("prompt", promptObj);
            requestBody.put("input", input);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("size", "2K");
            parameters.put("n", 1);
            parameters.put("watermark", false);
            requestBody.put("parameters", parameters);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("===== 阿里云异步生图请求 =====");
            log.info("URL: {}", DASHSCOPE_ASYNC_URL);
            log.info("请求体：{}", jsonBody);
            log.info("================================");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_ASYNC_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-DashScope-Async", "enable")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            log.info("===== 阿里云异步生图 API 响应 =====");
            log.info("状态码：{}", response.statusCode());
            log.info("响应体：{}", responseBody);
            log.info("================================");

            JsonNode root = objectMapper.readTree(responseBody);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = root.path("message").asText();
                if (!StringUtils.hasText(message)) {
                    message = root.path("code").asText("提交任务失败");
                }
                log.warn("万相异步生图提交失败 status={}, body={}", response.statusCode(), response.body());
                return Result.error("提交任务失败：" + message);
            }

            String taskId = root.path("output").path("task_id").asText();
            if (!StringUtils.hasText(taskId)) {
                log.warn("万相异步生图返回中未解析到 task_id: {}", response.body());
                return Result.error("提交失败：未获取到 task_id");
            }

            log.info("异步生图任务提交成功，taskId: {}", taskId);
            return Result.success("任务提交成功", taskId);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("调用万相异步生图失败", e);
            return Result.error("提交任务失败：" + e.getMessage());
        }
    }

    /**
     * 查询异步任务状态
     */
    @RequestMapping(value = "/wan/askstate/{taskid}", method = {RequestMethod.GET})
    public Result<?> askState(@PathVariable("taskid") String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return Result.error("taskId 不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            return Result.error("未配置 DASHSCOPE_API_KEY");
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_TASK_URL + taskId))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            log.info("===== 阿里云任务状态查询响应 =====");
            log.info("状态码：{}", response.statusCode());
            log.info("响应体：{}", responseBody);
            log.info("================================");

            JsonNode root = objectMapper.readTree(responseBody);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = root.path("message").asText();
                if (!StringUtils.hasText(message)) {
                    message = root.path("code").asText("查询任务失败");
                }
                log.warn("查询任务状态失败 status={}, body={}", response.statusCode(), response.body());
                return Result.error("查询失败：" + message);
            }

            // 解析任务状态
            String taskStatus = root.path("output").path("task_status").asText();
            Map<String, Object> data = new HashMap<>();
            data.put("task_status", taskStatus);
            data.put("task_id", taskId);

            // 如果任务成功，解析图片 URL
            if ("SUCCEEDED".equals(taskStatus)) {
                String imageUrl = findFirstImageUrl(root);
                if (StringUtils.hasText(imageUrl)) {
                    data.put("image_url", imageUrl);
                    log.info("任务完成，图片 URL: {}", imageUrl);
                } else {
                    log.warn("任务成功但未解析到图片 URL: {}", response.body());
                }
            }

            return Result.success("查询成功", data);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("查询任务状态失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @RequestMapping(value = "/qwen/txt2txt", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<?> txt2txt(@RequestParam("prompt") String prompt) {
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
                    "model", "qwen-plus",
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "你是一个优秀的博文作者，擅长对博客博文进行美化和扩写，让读者感到吸引和共鸣，你需要根据用户提供的文案进行扩写并进行美化修饰，返回中文内容，不要有额外的解释，只需要最终内容"
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_CHAT_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            log.info("===== 阿里云文案扩写 API 响应 =====");
            log.info("状态码：{}", response.statusCode());
            log.info("响应体：{}", responseBody);
            log.info("================================");

            JsonNode root = objectMapper.readTree(responseBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = root.path("message").asText();
                if (!StringUtils.hasText(message)) {
                    message = root.path("error").path("message").asText();
                }
                if (!StringUtils.hasText(message)) {
                    message = root.path("code").asText("AI 文案生成失败");
                }
                log.warn("通义文案扩写失败 status={}, body={}", response.statusCode(), responseBody);
                return Result.error("AI 文案生成失败：" + message);
            }

            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                log.warn("通义文案扩写返回内容为空: {}", responseBody);
                return Result.error("文案生成失败：未获取到有效内容");
            }

            return Result.success("文案生成成功", content);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("调用通义文案扩写失败", e);
            return Result.error("调用 AI 文案生成失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("处理通义文案扩写响应失败", e);
            return Result.error("AI 文案生成失败：" + e.getMessage());
        }
    }

    private String findFirstImageUrl(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }

        // 按照阿里云万相 API 返回结构解析：output.choices[0].message.content[0].image
        try {
            JsonNode output = root.get("output");
            if (output != null && !output.isNull()) {
                JsonNode choices = output.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice != null && !firstChoice.isNull()) {
                        JsonNode message = firstChoice.get("message");
                        if (message != null && !message.isNull()) {
                            JsonNode content = message.get("content");
                            if (content != null && content.isArray() && content.size() > 0) {
                                JsonNode firstContent = content.get(0);
                                if (firstContent != null && !firstContent.isNull()) {
                                    JsonNode imageNode = firstContent.get("image");
                                    if (imageNode != null && imageNode.isTextual()) {
                                        String imageUrl = imageNode.asText();
                                        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                                            log.info("成功解析图片 URL: {}", imageUrl);
                                            return imageUrl;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析图片 URL 时发生异常：{}", e.getMessage());
        }

        // 备用方案：递归查找任意 image 或 url 字段
        return findImageUrlRecursive(root);
    }

    private String findImageUrlRecursive(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            // 查找 image 字段
            JsonNode imageNode = node.get("image");
            if (imageNode != null && imageNode.isTextual()) {
                String url = imageNode.asText();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return url;
                }
            }
            // 查找 url 字段
            JsonNode urlNode = node.get("url");
            if (urlNode != null && urlNode.isTextual()) {
                String url = urlNode.asText();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return url;
                }
            }
            // 递归查找子节点
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String result = findImageUrlRecursive(entry.getValue());
                if (StringUtils.hasText(result)) {
                    return result;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                String result = findImageUrlRecursive(item);
                if (StringUtils.hasText(result)) {
                    return result;
                }
            }
        }

        return null;
    }
}
