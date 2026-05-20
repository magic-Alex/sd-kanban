package com.sdkanban.dashboard.repository;

import com.sdkanban.dashboard.dto.DashboardActivityResponse;
import com.sdkanban.dashboard.dto.MemberWorkloadResponse;
import com.sdkanban.dashboard.dto.SprintProgressResponse;
import com.sdkanban.dashboard.dto.TaskTypeDistributionResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.user.dto.UserSummary;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Conditional(ProjectPersistenceAvailableCondition.class)
public class DashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countPendingTasks(Long userId) {
        return count("""
            SELECT COUNT(*)
            FROM tasks task
            JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            JOIN project_members member
              ON member.project_id = task.project_id
             AND member.user_id = ?
            WHERE task.assignee_id = ?
              AND task.is_deleted = FALSE
              AND task.is_archived = FALSE
              AND column_state.is_done = FALSE
            """, userId, userId);
    }

    public int countOverdueTasks(Long userId) {
        return count("""
            SELECT COUNT(*)
            FROM tasks task
            JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            JOIN project_members member
              ON member.project_id = task.project_id
             AND member.user_id = ?
            WHERE task.assignee_id = ?
              AND task.is_deleted = FALSE
              AND task.is_archived = FALSE
              AND column_state.is_done = FALSE
              AND task.due_date < CURDATE()
            """, userId, userId);
    }

    public int countOwnedProjects(Long userId) {
        return count("SELECT COUNT(*) FROM projects WHERE owner_id = ?", userId);
    }

    public int countJoinedProjects(Long userId) {
        return count("""
            SELECT COUNT(DISTINCT project.id)
            FROM projects project
            JOIN project_members member
              ON member.project_id = project.id
            WHERE member.user_id = ?
              AND project.owner_id <> ?
            """, userId, userId);
    }

    public List<DashboardActivityResponse> findRecentActivities(Long userId, int limit) {
        return jdbcTemplate.query("""
            SELECT activity.id,
                   activity.task_id,
                   activity.project_id,
                   project.name AS project_name,
                   task.title AS task_title,
                   activity.actor_id,
                   actor.account AS actor_account,
                   actor.nickname AS actor_nickname,
                   actor.email AS actor_email,
                   actor.avatar_url AS actor_avatar_url,
                   activity.action_type,
                   activity.field_name,
                   activity.old_value,
                   activity.new_value,
                   activity.created_at
            FROM task_activities activity
            JOIN project_members member
              ON member.project_id = activity.project_id
             AND member.user_id = ?
            JOIN projects project
              ON project.id = activity.project_id
            JOIN tasks task
              ON task.id = activity.task_id
             AND task.project_id = activity.project_id
            LEFT JOIN users actor
              ON actor.id = activity.actor_id
            WHERE task.is_deleted = FALSE
            ORDER BY activity.created_at DESC, activity.id DESC
            LIMIT ?
            """, (rs, rowNum) -> new DashboardActivityResponse(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getLong("project_id"),
                rs.getString("project_name"),
                rs.getString("task_title"),
                userSummary(rs, "actor"),
                rs.getString("action_type"),
                rs.getString("field_name"),
                rs.getString("old_value"),
                rs.getString("new_value"),
                localDateTime(rs, "created_at")
            ), userId, limit);
    }

    public List<TrendCount> countCompletedTasksByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query("""
            SELECT DATE(task.updated_at) AS bucket_date,
                   COUNT(*) AS completed_count
            FROM tasks task
            JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            JOIN project_members member
              ON member.project_id = task.project_id
             AND member.user_id = ?
            WHERE task.is_deleted = FALSE
              AND task.is_archived = FALSE
              AND column_state.is_done = TRUE
              AND DATE(task.updated_at) BETWEEN ? AND ?
            GROUP BY DATE(task.updated_at)
            ORDER BY bucket_date
            """, (rs, rowNum) -> new TrendCount(
                rs.getDate("bucket_date").toLocalDate(),
                rs.getInt("completed_count")
            ), userId, Date.valueOf(startDate), Date.valueOf(endDate));
    }

    public ProjectTaskCounts projectTaskCounts(Long projectId) {
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(task.id) AS total_task_count,
                   COALESCE(SUM(CASE WHEN column_state.is_done = TRUE THEN 1 ELSE 0 END), 0) AS done_task_count
            FROM tasks task
            JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            WHERE task.project_id = ?
              AND task.is_deleted = FALSE
              AND task.is_archived = FALSE
            """, (rs, rowNum) -> taskCounts(
                rs.getInt("total_task_count"),
                rs.getInt("done_task_count")
            ), projectId);
    }

    public List<SprintProgressResponse> findSprintProgress(Long projectId) {
        return jdbcTemplate.query("""
            SELECT sprint.id AS sprint_id,
                   sprint.name,
                   COUNT(task.id) AS total_task_count,
                   COALESCE(SUM(CASE WHEN column_state.is_done = TRUE THEN 1 ELSE 0 END), 0) AS done_task_count
            FROM sprints sprint
            LEFT JOIN tasks task
              ON task.sprint_id = sprint.id
             AND task.project_id = sprint.project_id
             AND task.is_deleted = FALSE
             AND task.is_archived = FALSE
            LEFT JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            WHERE sprint.project_id = ?
            GROUP BY sprint.id, sprint.name, sprint.created_at
            ORDER BY sprint.created_at DESC, sprint.id DESC
            """, this::sprintProgress, projectId);
    }

    public Optional<SprintProgressResponse> findSprintProgress(Long projectId, Long sprintId) {
        List<SprintProgressResponse> rows = jdbcTemplate.query("""
            SELECT sprint.id AS sprint_id,
                   sprint.name,
                   COUNT(task.id) AS total_task_count,
                   COALESCE(SUM(CASE WHEN column_state.is_done = TRUE THEN 1 ELSE 0 END), 0) AS done_task_count
            FROM sprints sprint
            LEFT JOIN tasks task
              ON task.sprint_id = sprint.id
             AND task.project_id = sprint.project_id
             AND task.is_deleted = FALSE
             AND task.is_archived = FALSE
            LEFT JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            WHERE sprint.project_id = ?
              AND sprint.id = ?
            GROUP BY sprint.id, sprint.name
            """, this::sprintProgress, projectId, sprintId);
        return rows.stream().findFirst();
    }

    public List<TaskTypeDistributionResponse> findTaskTypeDistribution(Long projectId) {
        return jdbcTemplate.query("""
            SELECT task.task_type AS type,
                   COUNT(*) AS task_count
            FROM tasks task
            WHERE task.project_id = ?
              AND task.is_deleted = FALSE
              AND task.is_archived = FALSE
            GROUP BY task.task_type
            ORDER BY task.task_type
            """, (rs, rowNum) -> new TaskTypeDistributionResponse(
                rs.getString("type"),
                rs.getInt("task_count")
            ), projectId);
    }

    public List<MemberWorkloadResponse> findMemberWorkload(Long projectId) {
        return jdbcTemplate.query("""
            SELECT user_account.id AS user_id,
                   user_account.account AS user_account,
                   user_account.nickname AS user_nickname,
                   user_account.email AS user_email,
                   user_account.avatar_url AS user_avatar_url,
                   COALESCE(SUM(CASE WHEN task.id IS NOT NULL AND column_state.is_done = FALSE THEN 1 ELSE 0 END), 0) AS open_task_count,
                   COALESCE(SUM(CASE WHEN task.id IS NOT NULL AND column_state.is_done = TRUE THEN 1 ELSE 0 END), 0) AS done_task_count
            FROM project_members member
            JOIN users user_account
              ON user_account.id = member.user_id
            LEFT JOIN tasks task
              ON task.project_id = member.project_id
             AND task.assignee_id = member.user_id
             AND task.is_deleted = FALSE
             AND task.is_archived = FALSE
            LEFT JOIN board_columns column_state
              ON column_state.id = task.column_id
             AND column_state.project_id = task.project_id
            WHERE member.project_id = ?
            GROUP BY user_account.id,
                     user_account.account,
                     user_account.nickname,
                     user_account.email,
                     user_account.avatar_url
            ORDER BY user_account.id
            """, (rs, rowNum) -> {
                int openTaskCount = rs.getInt("open_task_count");
                int doneTaskCount = rs.getInt("done_task_count");
                return new MemberWorkloadResponse(
                    userSummary(rs, "user"),
                    openTaskCount,
                    doneTaskCount,
                    openTaskCount + doneTaskCount
                );
            }, projectId);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private ProjectTaskCounts taskCounts(int totalTaskCount, int doneTaskCount) {
        return new ProjectTaskCounts(totalTaskCount, doneTaskCount, totalTaskCount - doneTaskCount);
    }

    private SprintProgressResponse sprintProgress(ResultSet rs, int rowNum) throws SQLException {
        int totalTaskCount = rs.getInt("total_task_count");
        int doneTaskCount = rs.getInt("done_task_count");
        return new SprintProgressResponse(
            rs.getLong("sprint_id"),
            rs.getString("name"),
            totalTaskCount,
            doneTaskCount,
            totalTaskCount - doneTaskCount
        );
    }

    private UserSummary userSummary(ResultSet rs, String prefix) throws SQLException {
        Object id = rs.getObject(prefix + "_id");
        if (id == null) {
            return null;
        }
        return new UserSummary(
            ((Number) id).longValue(),
            rs.getString(prefix + "_account"),
            rs.getString(prefix + "_nickname"),
            rs.getString(prefix + "_email"),
            rs.getString(prefix + "_avatar_url")
        );
    }

    private LocalDateTime localDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public record TrendCount(LocalDate date, int completedCount) {
    }

    public record ProjectTaskCounts(int totalTaskCount, int doneTaskCount, int openTaskCount) {
    }
}
