package com.sdkanban.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_checklist_items")
public class TaskChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TaskChecklistItem() {
    }

    public TaskChecklistItem(Long taskId, Long projectId, String title, Integer sortOrder, Long createdBy) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.title = title;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDone() {
        return done;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getCompletedBy() {
        return completedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void markDone(Long userId) {
        this.done = true;
        this.completedBy = userId;
        this.completedAt = LocalDateTime.now();
    }

    public void markOpen() {
        this.done = false;
        this.completedBy = null;
        this.completedAt = null;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
