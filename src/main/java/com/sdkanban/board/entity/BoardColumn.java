package com.sdkanban.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_columns")
public class BoardColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "template_key", nullable = false, length = 60)
    private String templateKey;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BoardColumn() {
    }

    public BoardColumn(
        Long projectId,
        String templateKey,
        String name,
        String color,
        Integer sortOrder,
        Integer wipLimit,
        boolean done
    ) {
        this.projectId = projectId;
        this.templateKey = templateKey;
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    public BoardColumn(
        Long projectId,
        String name,
        String color,
        Integer sortOrder,
        Integer wipLimit,
        boolean done
    ) {
        this(projectId, defaultTemplateKey(), name, color, sortOrder, wipLimit, done);
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Integer getWipLimit() {
        return wipLimit;
    }

    public boolean isDone() {
        return done;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String color, Integer wipLimit, boolean done) {
        this.name = name;
        this.color = color;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void syncFromTemplate(String name, String color, Integer sortOrder, Integer wipLimit, boolean done) {
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    private static String defaultTemplateKey() {
        return "CUSTOM_" + UUID.randomUUID();
    }
}
