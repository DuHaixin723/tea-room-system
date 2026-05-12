package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
/**
 * FileController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件上传")
public class FileController {

    private final ImageStorageService imageStorageService;

    @PostMapping("/images")
    @Operation(summary = "上传图片")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(defaultValue = "images") String folder) {
        String imageUrl = imageStorageService.storeImage(file, folder);
        return ApiResponse.ok(Map.of("url", imageUrl));
    }

    @PostMapping("/public-images")
    @Operation(summary = "上传公开审核资料图片")
    public ApiResponse<Map<String, String>> uploadPublicImage(@RequestParam("file") MultipartFile file,
                                                              @RequestParam(defaultValue = "staff-applications") String folder) {
        String imageUrl = imageStorageService.storeImage(file, folder);
        return ApiResponse.ok(Map.of("url", imageUrl));
    }
}
