package com.sdkanban.dashboard.service.impl;

import com.sdkanban.common.BusinessException;
import com.sdkanban.dashboard.dto.CompletionTrendBucket;
import com.sdkanban.dashboard.dto.DashboardSummaryResponse;
import com.sdkanban.dashboard.dto.DashboardTrendsResponse;
import com.sdkanban.dashboard.dto.ProjectStatsResponse;
import com.sdkanban.dashboard.dto.SprintProgressResponse;
import com.sdkanban.dashboard.repository.DashboardRepository;
import com.sdkanban.dashboard.service.DashboardService;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class DashboardServiceImpl implements DashboardService {
    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final int TREND_DAYS = 7;

    private final DashboardRepository dashboardRepository;
    private final ProjectService projectService;

    public DashboardServiceImpl(DashboardRepository dashboardRepository, ProjectService projectService) {
        this.dashboardRepository = dashboardRepository;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(Long currentUserId) {
        return new DashboardSummaryResponse(
            dashboardRepository.countPendingTasks(currentUserId),
            dashboardRepository.countOverdueTasks(currentUserId),
            dashboardRepository.countOwnedProjects(currentUserId),
            dashboardRepository.countJoinedProjects(currentUserId),
            dashboardRepository.findRecentActivities(currentUserId, RECENT_ACTIVITY_LIMIT)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardTrendsResponse trends(Long currentUserId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(TREND_DAYS - 1L);
        Map<LocalDate, Integer> completedByDate = dashboardRepository.countCompletedTasksByDate(
                currentUserId,
                startDate,
                endDate
            )
            .stream()
            .collect(Collectors.toMap(
                DashboardRepository.TrendCount::date,
                DashboardRepository.TrendCount::completedCount
            ));

        List<CompletionTrendBucket> buckets = IntStream.range(0, TREND_DAYS)
            .mapToObj(startDate::plusDays)
            .map(date -> new CompletionTrendBucket(date, completedByDate.getOrDefault(date, 0)))
            .toList();
        return new DashboardTrendsResponse(buckets);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectStatsResponse projectStats(Long projectId, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        DashboardRepository.ProjectTaskCounts counts = dashboardRepository.projectTaskCounts(projectId);
        return new ProjectStatsResponse(
            projectId,
            counts.totalTaskCount(),
            counts.doneTaskCount(),
            counts.openTaskCount(),
            dashboardRepository.findSprintProgress(projectId),
            dashboardRepository.findTaskTypeDistribution(projectId),
            dashboardRepository.findMemberWorkload(projectId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SprintProgressResponse sprintStats(Long projectId, Long sprintId, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        return dashboardRepository.findSprintProgress(projectId, sprintId)
            .orElseThrow(() -> BusinessException.notFound("SPRINT_NOT_FOUND", "Sprint not found"));
    }
}
