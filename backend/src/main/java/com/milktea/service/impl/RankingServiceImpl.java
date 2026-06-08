package com.milktea.service.impl;

import com.milktea.dto.CategoryPreferenceVO;
import com.milktea.dto.HotProductVO;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RankingServiceImpl implements RankingService {

    private static final Logger logger = LoggerFactory.getLogger(RankingServiceImpl.class);
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int DEFAULT_LIMIT = 10;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    private OrderItemMapper orderItemMapper;

    private static class CacheEntry {
        final List<HotProductVO> data;
        final long timestamp;

        CacheEntry(List<HotProductVO> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @Override
    public List<HotProductVO> getHotRanking(int days, Long categoryId, int limit) {
        String cacheKey = "hot:" + days + ":" + categoryId + ":" + limit;
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            logger.debug("榜单缓存命中: key={}", cacheKey);
            return entry.data;
        }

        logger.info("榜单缓存未命中，查询数据库: days={}, categoryId={}, limit={}", days, categoryId, limit);
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<HotProductVO> result = orderItemMapper.selectHotProducts(startDate, categoryId, limit);
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    @Override
    public List<HotProductVO> getRecommendation(Long userId, int limit) {
        if (userId == null) {
            return getHotRanking(7, null, limit);
        }

        String cacheKey = "recommend:" + userId + ":" + limit;
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            logger.debug("推荐缓存命中: key={}", cacheKey);
            return entry.data;
        }

        logger.info("推荐缓存未命中，查询数据库: userId={}, limit={}", userId, limit);
        List<CategoryPreferenceVO> preferences = orderItemMapper.selectUserCategoryPreference(userId);

        if (preferences == null || preferences.isEmpty()) {
            logger.info("用户无历史订单，返回全站热销: userId={}", userId);
            return getHotRanking(7, null, limit);
        }

        List<Long> topCategoryIds = preferences.stream()
                .limit(3)
                .map(CategoryPreferenceVO::getCategoryId)
                .collect(Collectors.toList());

        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<HotProductVO> result = orderItemMapper.selectHotProductsByCategories(startDate, topCategoryIds, limit);

        if (result.isEmpty()) {
            logger.info("用户偏好品类无热销商品，返回全站热销: userId={}", userId);
            return getHotRanking(7, null, limit);
        }

        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000L)
    public void cleanExpiredCache() {
        int before = cache.size();
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
        int after = cache.size();
        if (before != after) {
            logger.info("清理过期缓存: 清理前={}, 清理后={}", before, after);
        }
    }
}
