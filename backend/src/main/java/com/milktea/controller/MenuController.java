package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.Category;
import com.milktea.entity.Product;
import com.milktea.mapper.CategoryMapper;
import com.milktea.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MenuController {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        return Result.success(categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }

    @GetMapping("/products")
    public Result<Page<Product>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1);
        
        if (categoryId != null) {
            query.eq(Product::getCategoryId, categoryId);
        }
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(q -> q
                    .like(Product::getName, keyword)
                    .or()
                    .like(Product::getDescription, keyword));
        }
        
        applySorting(query, sortBy, sortOrder);
        
        return Result.success(productMapper.selectPage(pageParam, query));
    }
    
    private void applySorting(LambdaQueryWrapper<Product> query, String sortBy, String sortOrder) {
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        
        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "price":
                    if (isAsc) {
                        query.orderByAsc(Product::getPrice);
                    } else {
                        query.orderByDesc(Product::getPrice);
                    }
                    break;
                case "name":
                    if (isAsc) {
                        query.orderByAsc(Product::getName);
                    } else {
                        query.orderByDesc(Product::getName);
                    }
                    break;
                case "createtime":
                    if (isAsc) {
                        query.orderByAsc(Product::getCreateTime);
                    } else {
                        query.orderByDesc(Product::getCreateTime);
                    }
                    break;
                case "stock":
                    if (isAsc) {
                        query.orderByAsc(Product::getStock);
                    } else {
                        query.orderByDesc(Product::getStock);
                    }
                    break;
                default:
                    query.orderByDesc(Product::getCreateTime);
                    break;
            }
        } else {
            query.orderByDesc(Product::getCreateTime);
        }
    }
}
