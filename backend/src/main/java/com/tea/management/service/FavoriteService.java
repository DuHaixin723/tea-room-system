package com.tea.management.service;

import com.tea.management.domain.entity.Favorite;
import com.tea.management.dto.request.FavoriteRequest;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
/**
 * FavoriteService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public Favorite create(FavoriteRequest request) {
        var existing = favoriteRepository.findByUserIdAndTargetIdAndTargetType(
                request.userId(),
                request.targetId(),
                request.targetType()
        );
        if (existing.isPresent()) {
            return existing.get();
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(request.userId());
        favorite.setTargetId(request.targetId());
        favorite.setTargetType(request.targetType());
        return favoriteRepository.save(favorite);
    }

    public Page<Favorite> listByUser(Long userId, Pageable pageable) {
        return favoriteRepository.findByUserId(userId, pageable);
    }

    public Favorite requireFavorite(Long id) {
        return favoriteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("收藏不存在"));
    }

    public void delete(Long id) {
        favoriteRepository.deleteById(id);
    }
}
