package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.ResultCode;
import com.milktea.entity.Favorite;
import com.milktea.entity.Product;
import com.milktea.mapper.FavoriteMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Favorite addFavorite(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException(ResultCode.PRODUCT_NOT_FOUND.getMessage());
        }

        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (existing != null) {
            return existing;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            favoriteMapper.insert(favorite);
            logger.info("添加收藏: userId={}, productId={}", userId, productId);
        } catch (DuplicateKeyException e) {
            logger.info("并发重复收藏，返回已有记录: userId={}, productId={}", userId, productId);
            return favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getProductId, productId));
        }
        return favorite;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long productId) {
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (deleted > 0) {
            logger.info("取消收藏: userId={}, productId={}", userId, productId);
        }
    }

    @Override
    public Page<Favorite> getFavoriteList(Long userId, Integer page, Integer pageSize) {
        Page<Favorite> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime);
        return favoriteMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public boolean isFavorite(Long userId, Long productId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0;
    }

    @Override
    public List<Long> getFavoriteProductIds(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .in(Favorite::getProductId, productIds));
        return favorites.stream().map(Favorite::getProductId).toList();
    }
}
