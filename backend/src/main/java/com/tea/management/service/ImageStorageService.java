package com.tea.management.service;

import com.tea.management.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
/**
 * ImageStorageService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
public class ImageStorageService {

    private static final long MAX_FILE_SIZE = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final Path uploadRoot;

    public ImageStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片不能超过 8MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }

        String safeFolder = sanitizeFolder(folder);
        String extension = resolveExtension(file.getOriginalFilename());
        Path folderPath = uploadRoot.resolve(safeFolder).normalize();

        try {
            Files.createDirectories(folderPath);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = folderPath.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + safeFolder + "/" + fileName;
        } catch (IOException ex) {
            throw new BusinessException("图片上传失败");
        }
    }

    private String sanitizeFolder(String folder) {
        String normalized = (folder == null || folder.isBlank()) ? "images" : folder.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9\\-/]+")) {
            throw new BusinessException("图片目录不合法");
        }
        return normalized;
    }

    private String resolveExtension(String originalFilename) {
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String normalized = ext == null ? "png" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new BusinessException("图片格式仅支持 jpg、png、webp、gif");
        }
        return normalized;
    }
}
