package com.weibo.controller;

import com.weibo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/upload")
@Slf4j
public class UploadController {

    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFileSize;

    // 上传目录
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    /**
     * 上传图片
     */
    @PostMapping("/images")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 验证文件大小 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过 5MB");
        }

        try {
            Path uploadPath = ensureUploadDir();

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = resolveExtension(originalFilename, contentType);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // 返回访问 URL（相对路径）
            String imgUrl = "/uploads/" + filename;

            Map<String, String> data = new HashMap<>();
            data.put("imgUrl", imgUrl);
            data.put("filename", filename);

            log.info("图片上传成功：{}", filename);
            return Result.success("上传成功", data);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传图片
     */
    @PostMapping("/images/batch")
    public Result<?> uploadImages(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("请选择要上传的文件");
        }

        Map<String, Object> data = new HashMap<>();
        List<String> imageUrls = new ArrayList<>();

        try {
            Path uploadPath = ensureUploadDir();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("image/")) {
                        String originalFilename = file.getOriginalFilename();
                        String extension = resolveExtension(originalFilename, contentType);
                        String filename = UUID.randomUUID().toString().replace("-", "") + extension;

                        Path filePath = uploadPath.resolve(filename);
                        file.transferTo(filePath.toFile());

                        imageUrls.add("/uploads/" + filename);
                    }
                }
            }

            data.put("imageUrls", imageUrls);
            return Result.success("上传成功", data);
        } catch (IOException e) {
            log.error("批量上传图片失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 网络图片转存本地
     */
    @RequestMapping(value = "/imagesByUrl", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<?> uploadImageByUrl(@RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            return Result.error("url 不能为空");
        }

        HttpURLConnection connection = null;
        try {
            Path uploadPath = ensureUploadDir();

            URL fileUrl = new URL(url);
            connection = (HttpURLConnection) fileUrl.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                return Result.error("下载图片失败，响应码：" + responseCode);
            }

            String contentType = connection.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                return Result.error("URL 对应的资源不是图片");
            }

            String extension = resolveExtensionFromUrl(url, contentType);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetFile = uploadPath.resolve(filename);

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String imgUrl = "/uploads/" + filename;
            Map<String, String> data = new HashMap<>();
            data.put("imgUrl", imgUrl);
            data.put("filename", filename);
            data.put("sourceUrl", url);

            log.info("网络图片转存成功：{} -> {}", url, filename);
            return Result.success("转存成功", data);
        } catch (IOException e) {
            log.error("网络图片转存失败：{}", url, e);
            return Result.error("转存失败：" + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Path ensureUploadDir() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return extensionFromContentType(contentType);
    }

    private String resolveExtensionFromUrl(String url, String contentType) {
        String cleanUrl = url;
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex >= 0) {
            cleanUrl = cleanUrl.substring(0, queryIndex);
        }
        int dotIndex = cleanUrl.lastIndexOf('.');
        int slashIndex = cleanUrl.lastIndexOf('/');
        if (dotIndex > slashIndex) {
            return cleanUrl.substring(dotIndex);
        }
        return extensionFromContentType(contentType);
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            case "image/jpeg", "image/jpg" -> ".jpg";
            default -> ".jpg";
        };
    }
}
