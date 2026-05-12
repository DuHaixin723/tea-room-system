package com.tea.management.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
/**
 * PageResponse 通用数据结构，用来统一系统中的公共返回格式。
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
