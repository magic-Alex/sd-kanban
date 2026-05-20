package com.sdkanban.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskTagLinkId implements Serializable {
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "project_id")
    private Long projectId;

    protected TaskTagLinkId() {
    }

    public TaskTagLinkId(Long taskId, Long tagId, Long projectId) {
        this.taskId = taskId;
        this.tagId = tagId;
        this.projectId = projectId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getTagId() {
        return tagId;
    }

    public Long getProjectId() {
        return projectId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TaskTagLinkId that)) {
            return false;
        }
        return Objects.equals(taskId, that.taskId)
            && Objects.equals(tagId, that.tagId)
            && Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, tagId, projectId);
    }
}
