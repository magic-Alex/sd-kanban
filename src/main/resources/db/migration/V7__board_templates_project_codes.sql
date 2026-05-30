ALTER TABLE projects
    ADD COLUMN project_code VARCHAR(40) NULL AFTER creator_id,
    ADD COLUMN project_color VARCHAR(20) NOT NULL DEFAULT '#0f766e' AFTER project_code;

UPDATE projects
SET project_code = CONCAT('PRJ-', id)
WHERE project_code IS NULL;

ALTER TABLE projects
    MODIFY project_code VARCHAR(40) NOT NULL DEFAULT (UUID()),
    ADD UNIQUE KEY uk_projects_project_code (project_code);

ALTER TABLE board_columns
    ADD COLUMN template_key VARCHAR(60) NULL AFTER project_id,
    ADD KEY idx_board_columns_template_key (template_key);

UPDATE board_columns
SET template_key = CONCAT('CUSTOM_', id)
WHERE template_key IS NULL;

UPDATE board_columns column_table
JOIN (
    SELECT id, template_key
    FROM (
        SELECT
            id,
            template_key,
            ROW_NUMBER() OVER (
                PARTITION BY project_id, template_key
                ORDER BY sort_order, id
            ) AS template_rank
        FROM (
            SELECT
                id,
                project_id,
                sort_order,
                CASE
                    WHEN sort_order = 0 AND name = 'Backlog' AND color = '#64748b' AND wip_limit IS NULL AND is_done = false THEN 'BACKLOG'
                    WHEN sort_order = 1 AND name = 'Ready' AND color = '#0ea5e9' AND wip_limit IS NULL AND is_done = false THEN 'READY'
                    WHEN sort_order = 2 AND name = 'In Progress' AND color = '#f59e0b' AND wip_limit IS NULL AND is_done = false THEN 'IN_PROGRESS'
                    WHEN sort_order = 3 AND name = 'Testing' AND color = '#8b5cf6' AND wip_limit IS NULL AND is_done = false THEN 'TESTING'
                    WHEN sort_order = 4 AND name = 'Done' AND color = '#22c55e' AND wip_limit IS NULL AND is_done = true THEN 'DONE'
                    ELSE NULL
                END AS template_key
            FROM board_columns
        ) template_candidates
        WHERE template_key IS NOT NULL
    ) ranked_templates
    WHERE template_rank = 1
) canonical_templates ON canonical_templates.id = column_table.id
SET column_table.template_key = canonical_templates.template_key;

ALTER TABLE board_columns
    MODIFY template_key VARCHAR(60) NOT NULL,
    ADD UNIQUE KEY uk_board_columns_project_template (project_id, template_key);

CREATE TABLE IF NOT EXISTS board_column_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_key VARCHAR(60) NOT NULL,
    name_zh VARCHAR(80) NOT NULL,
    name_en VARCHAR(80) NOT NULL,
    color VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL,
    wip_limit INT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_board_column_templates_key (template_key),
    UNIQUE KEY uk_board_column_templates_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO board_column_templates (template_key, name_zh, name_en, color, sort_order, wip_limit, is_done)
VALUES
    ('BACKLOG', '待办', 'Backlog', '#64748b', 0, NULL, false),
    ('READY', '就绪', 'Ready', '#0ea5e9', 1, NULL, false),
    ('IN_PROGRESS', '进行中', 'In Progress', '#f59e0b', 2, NULL, false),
    ('TESTING', '测试', 'Testing', '#8b5cf6', 3, NULL, false),
    ('DONE', '完成', 'Done', '#22c55e', 4, NULL, true)
ON DUPLICATE KEY UPDATE
    name_zh = VALUES(name_zh),
    name_en = VALUES(name_en),
    color = VALUES(color),
    sort_order = VALUES(sort_order),
    wip_limit = VALUES(wip_limit),
    is_done = VALUES(is_done);

UPDATE board_columns column_table
JOIN board_column_templates template_table ON template_table.template_key = column_table.template_key
SET column_table.name = CONCAT(template_table.name_zh, '（', template_table.name_en, '）'),
    column_table.color = template_table.color,
    column_table.sort_order = template_table.sort_order,
    column_table.wip_limit = template_table.wip_limit,
    column_table.is_done = template_table.is_done;
