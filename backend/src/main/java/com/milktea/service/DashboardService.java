package com.milktea.service;

import com.milktea.dto.DashboardVO;

public interface DashboardService {
    DashboardVO getDashboardData();
    void refreshCache();
}
