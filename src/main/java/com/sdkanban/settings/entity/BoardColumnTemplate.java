package com.sdkanban.settings.entity;

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
@Table(name = "board_column_templates")
public class BoardColumnTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, length = 60)
    private String templateKey;

    @Column(name = "name_zh", nullable = false, length = 80)
    private String nameZh;

    @Column(name = "name_en", nullable = false, length = 80)
    private String nameEn;

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

    protected BoardColumnTemplate() {
    }

    public BoardColumnTemplate(String templateKey, String nameZh, String nameEn, String color, Integer sortOrder, Integer wipLimit, boolean done) {
        this.templateKey = templateKey;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.color = color;
        this.sortOrder = sortOrder;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDisplayName() {
        return nameZh + "（" + nameEn + "）";
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

    public void update(String nameZh, String nameEn, String color, Integer wipLimit, boolean done) {
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.color = color;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
