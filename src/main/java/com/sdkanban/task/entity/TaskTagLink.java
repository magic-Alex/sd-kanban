package com.sdkanban.task.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_tag_links")
public class TaskTagLink {
    @EmbeddedId
    private TaskTagLinkId id;

    protected TaskTagLink() {
    }

    public TaskTagLink(Long taskId, Long tagId, Long projectId) {
        this.id = new TaskTagLinkId(taskId, tagId, projectId);
    }

    public TaskTagLinkId getId() {
        return id;
    }
}
