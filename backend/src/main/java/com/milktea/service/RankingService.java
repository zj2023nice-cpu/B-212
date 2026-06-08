package com.milktea.service;

import com.milktea.dto.HotProductVO;
import java.util.List;

public interface RankingService {
    List<HotProductVO> getHotRanking(int days, Long categoryId, int limit);
    List<HotProductVO> getRecommendation(Long userId, int limit);
}
