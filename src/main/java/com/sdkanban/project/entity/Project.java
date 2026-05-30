package com.sdkanban.project.entity;

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
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "project_code", nullable = false, length = 40)
    private String projectCode;

    @Column(name = "project_color", nullable = false, length = 20)
    private String projectColor;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Project() {
    }

    public Project(
        Long ownerId,
        Long creatorId,
        String projectCode,
        String projectColor,
        String name,
        String description
    ) {
        this.ownerId = ownerId;
        this.creatorId = creatorId;
        this.projectCode = projectCode;
        this.projectColor = projectColor;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void transferOwner(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectColor() {
        return projectColor;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
