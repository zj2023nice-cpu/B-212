package com.milktea.controller;

import com.milktea.common.Result;
import com.milktea.dto.DashboardVO;
import com.milktea.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardVO> getDashboardData() {
        return Result.success(dashboardService.getDashboardData());
    }
}
