package com.tea.management.service;

import com.tea.management.domain.entity.Tea;
import com.tea.management.dto.request.TeaRequest;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.TeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
/**
 * TeaService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class TeaService {

    private final TeaRepository teaRepository;

    public Page<Tea> list(String keyword, Boolean enabled, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return teaRepository.search(normalizedKeyword, enabled, pageable);
    }

    public Tea create(TeaRequest request) {
        Tea tea = new Tea();
        apply(tea, request);
        return teaRepository.save(tea);
    }

    public Tea update(Long id, TeaRequest request) {
        Tea tea = teaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("茶叶不存在"));
        apply(tea, request);
        return teaRepository.save(tea);
    }

    public void delete(Long id) {
        teaRepository.deleteById(id);
    }

    public Tea get(Long id) {
        return teaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("茶叶不存在"));
    }

    private void apply(Tea tea, TeaRequest request) {
        tea.setName(request.name());
        tea.setCategory(request.category());
        tea.setPrice(request.price());
        tea.setStock(request.stock());
        tea.setImageUrl(request.imageUrl());
        tea.setDescription(request.description());
        tea.setEnabled(request.enabled());
    }
}
