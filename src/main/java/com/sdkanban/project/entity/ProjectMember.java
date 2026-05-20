package com.sdkanban.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
public class ProjectMember {
    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_MEMBER = "member";

    @EmbeddedId
    private ProjectMemberId id;

    @Column(nullable = false, length = 32)
    private String role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProjectMember() {
    }

    public ProjectMember(Long projectId, Long userId, String role) {
        this.id = new ProjectMemberId(projectId, userId);
        this.role = role;
    }

    public ProjectMemberId getId() {
        return id;
    }

    public Long getProjectId() {
        return id.getProjectId();
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public String getRole() {
        return role;
    }

    public void changeRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
