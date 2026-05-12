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
 * AvatarStorageService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
public class AvatarStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final Path avatarRoot;

    public AvatarStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.avatarRoot = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
    }

    public String storeAvatar(MultipartFile file, String existingAvatarUrl) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("头像图片不能超过 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("头像只能上传图片文件");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        try {
            Files.createDirectories(avatarRoot);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = avatarRoot.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            deleteLocalAvatar(existingAvatarUrl);
            return "/uploads/avatars/" + fileName;
        } catch (IOException ex) {
            throw new BusinessException("头像上传失败");
        }
    }

    private String resolveExtension(String originalFilename) {
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String normalized = ext == null ? "png" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new BusinessException("头像格式仅支持 jpg、png、webp、gif");
        }
        return normalized;
    }

    private void deleteLocalAvatar(String existingAvatarUrl) {
        if (existingAvatarUrl == null || !existingAvatarUrl.startsWith("/uploads/avatars/")) {
            return;
        }
        String fileName = existingAvatarUrl.substring("/uploads/avatars/".length());
        if (fileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(avatarRoot.resolve(fileName).normalize());
        } catch (IOException ignored) {
        }
    }
}
