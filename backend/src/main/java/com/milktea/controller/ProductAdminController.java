package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.dto.ProductAdminVO;
import com.milktea.entity.Category;
import com.milktea.entity.Product;
import com.milktea.mapper.CategoryMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductService productService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<ProductAdminVO>> adminListProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            query.eq(Product::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(q -> q.like(Product::getName, keyword).or().like(Product::getDescription, keyword));
        }
        if (status != null) {
            query.eq(Product::getStatus, status);
        }
        query.orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(pageParam, query);

        Page<ProductAdminVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductAdminVO> voList = productPage.getRecords().stream().map(p -> {
            ProductAdminVO vo = new ProductAdminVO();
            vo.setId(p.getId());
            vo.setCategoryId(p.getCategoryId());
            vo.setName(p.getName());
            vo.setDescription(p.getDescription());
            vo.setPrice(p.getPrice());
            vo.setImage(p.getImage());
            vo.setStatus(p.getStatus());
            vo.setStock(p.getStock());
            vo.setLowStockThreshold(p.getLowStockThreshold());
            vo.setSpecPriceRules(p.getSpecPriceRules());
            int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10;
            vo.setLowStock(p.getStock() != null && p.getStock() < threshold);
            vo.setVersion(p.getVersion());
            vo.setCreateTime(p.getCreateTime());
            vo.setUpdateTime(p.getUpdateTime());
            return vo;
        }).toList();
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ProductAdminVO>> getLowStockProducts() {
        return Result.success(productService.getLowStockProducts());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Product> createProduct(@RequestBody Product product) {
        productService.save(product);
        return Result.success(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productService.updateById(product);
        return Result.success(product);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        productService.updateById(product);
        return Result.success("Status updated");
    }

    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Category>> getCategories() {
        return Result.success(categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }
}
