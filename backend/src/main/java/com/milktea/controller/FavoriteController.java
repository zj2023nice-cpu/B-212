package com.milktea.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.Favorite;
import com.milktea.entity.Product;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.FavoriteService;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductMapper productMapper;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    @PostMapping
    public Result<Favorite> addFavorite(@RequestBody Map<String, Long> params) {
        Long productId = params.get("productId");
        if (productId == null) {
            return Result.badRequest("商品ID不能为空");
        }
        Long userId = getCurrentUserId();
        Favorite favorite = favoriteService.addFavorite(userId, productId);
        return Result.success(favorite);
    }

    @DeleteMapping("/{productId}")
    public Result<String> removeFavorite(@PathVariable Long productId) {
        Long userId = getCurrentUserId();
        favoriteService.removeFavorite(userId, productId);
        return Result.success("取消收藏成功");
    }

    @GetMapping
    public Result<Map<String, Object>> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        Long userId = getCurrentUserId();
        Page<Favorite> favoritePage = favoriteService.getFavoriteList(userId, page, pageSize);

        List<Map<String, Object>> productList = favoritePage.getRecords().stream().map(fav -> {
            Product product = productMapper.selectById(fav.getProductId());
            Map<String, Object> item = new HashMap<>();
            if (product != null) {
                item.put("productId", product.getId());
                item.put("name", product.getName());
                item.put("description", product.getDescription());
                item.put("price", product.getPrice());
                item.put("image", product.getImage());
                item.put("status", product.getStatus());
            }
            item.put("favoriteId", fav.getId());
            item.put("favoriteTime", fav.getCreateTime());
            return item;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", productList);
        result.put("total", favoritePage.getTotal());
        result.put("current", favoritePage.getCurrent());
        result.put("pages", favoritePage.getPages());
        return Result.success(result);
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> checkFavorite(@RequestParam Long productId) {
        Long userId = getCurrentUserId();
        boolean favorited = favoriteService.isFavorite(userId, productId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        return Result.success(result);
    }

    @PostMapping("/batch-check")
    public Result<Map<Long, Boolean>> batchCheckFavorite(@RequestBody List<Long> productIds) {
        Long userId = getCurrentUserId();
        List<Long> favoriteIds = favoriteService.getFavoriteProductIds(userId, productIds);
        Map<Long, Boolean> result = new HashMap<>();
        for (Long pid : productIds) {
            result.put(pid, favoriteIds.contains(pid));
        }
        return Result.success(result);
    }
}
