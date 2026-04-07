package com.weibo.controller;

import com.weibo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
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
        java.util.List<String> imageUrls = new java.util.ArrayList<>();

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("image/")) {
                        String originalFilename = file.getOriginalFilename();
                        String extension = originalFilename != null && originalFilename.contains(".")
                                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                                : ".jpg";
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
}
