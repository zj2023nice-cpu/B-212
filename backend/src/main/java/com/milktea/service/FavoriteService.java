package com.milktea.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.entity.Favorite;
import java.util.List;

public interface FavoriteService {
    Favorite addFavorite(Long userId, Long productId);
    void removeFavorite(Long userId, Long productId);
    Page<Favorite> getFavoriteList(Long userId, Integer page, Integer pageSize);
    boolean isFavorite(Long userId, Long productId);
    List<Long> getFavoriteProductIds(Long userId, List<Long> productIds);
}
