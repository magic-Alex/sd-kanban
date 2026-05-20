package com.sdkanban.dashboard.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.dashboard.dto.DashboardSummaryResponse;
import com.sdkanban.dashboard.dto.DashboardTrendsResponse;
import com.sdkanban.dashboard.dto.ProjectStatsResponse;
import com.sdkanban.dashboard.dto.SprintProgressResponse;
import com.sdkanban.dashboard.service.DashboardService;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.user.entity.User;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/summary")
    ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(dashboardService.summary(currentUserId(user)));
    }

    @GetMapping("/dashboard/trends")
    ApiResponse<DashboardTrendsResponse> trends(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(dashboardService.trends(currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/stats")
    ApiResponse<ProjectStatsResponse> projectStats(
        @PathVariable Long projectId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(dashboardService.projectStats(projectId, currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/sprints/{sprintId}/stats")
    ApiResponse<SprintProgressResponse> sprintStats(
        @PathVariable Long projectId,
        @PathVariable Long sprintId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(dashboardService.sprintStats(projectId, sprintId, currentUserId(user)));
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
