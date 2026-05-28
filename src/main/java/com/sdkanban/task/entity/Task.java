package com.sdkanban.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    public static final String DEFAULT_TYPE = "TASK";
    public static final String DEFAULT_PRIORITY = "MEDIUM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "column_id", nullable = false)
    private Long columnId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType = DEFAULT_TYPE;

    @Column(nullable = false, length = 32)
    private String priority = DEFAULT_PRIORITY;

    @Column(name = "story_points")
    private BigDecimal storyPoints;

    @Column(name = "estimated_hours")
    private BigDecimal estimatedHours;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Task() {
    }

    public Task(
        Long projectId,
        Long sprintId,
        Long columnId,
        Long assigneeId,
        Long creatorId,
        String title,
        String description,
        String taskType,
        String priority,
        BigDecimal storyPoints,
        BigDecimal estimatedHours,
        LocalDate dueDate,
        String acceptanceCriteria,
        Integer sortOrder
    ) {
        this.projectId = projectId;
        this.sprintId = sprintId;
        this.columnId = columnId;
        this.assigneeId = assigneeId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.estimatedHours = estimatedHours;
        this.dueDate = dueDate;
        this.acceptanceCriteria = acceptanceCriteria;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public Long getColumnId() {
        return columnId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getPriority() {
        return priority;
    }

    public BigDecimal getStoryPoints() {
        return storyPoints;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void changePriority(String priority) {
        this.priority = priority;
    }

    public void changeStoryPoints(BigDecimal storyPoints) {
        this.storyPoints = storyPoints;
    }

    public void changeEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public void changeAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public void changeDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void changeAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void changeSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public void changeColumnId(Long columnId) {
        this.columnId = columnId;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void archive() {
        this.archived = true;
    }

    public void delete() {
        this.deleted = true;
    }
}
