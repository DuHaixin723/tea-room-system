package com.tea.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * StaticResourceConfig 配置类，用来声明系统运行时需要的框架配置。
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String uploadLocation;

    public StaticResourceConfig(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadLocation = uploadPath.toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation.endsWith("/") ? uploadLocation : uploadLocation + "/");
    }
}
