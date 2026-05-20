ALTER TABLE users
    ADD COLUMN account VARCHAR(64) NULL AFTER id,
    ADD COLUMN nickname VARCHAR(100) NULL AFTER account,
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER email,
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' AFTER password_hash;

UPDATE users
SET account = username,
    nickname = display_name
WHERE account IS NULL
   OR nickname IS NULL;

ALTER TABLE users
    MODIFY account VARCHAR(64) NOT NULL,
    MODIFY nickname VARCHAR(100) NOT NULL,
    ADD UNIQUE KEY uk_users_account (account);

ALTER TABLE projects
    ADD COLUMN creator_id BIGINT NULL AFTER owner_id,
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' AFTER description,
    ADD KEY idx_projects_creator_id (creator_id);

UPDATE projects
SET creator_id = owner_id
WHERE creator_id IS NULL;

ALTER TABLE projects
    MODIFY creator_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_projects_creator_id FOREIGN KEY (creator_id) REFERENCES users (id);

ALTER TABLE board_columns
    ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT '#64748b' AFTER name,
    ADD COLUMN is_done BOOLEAN NOT NULL DEFAULT FALSE AFTER wip_limit,
    ADD UNIQUE KEY uk_board_columns_id_project (id, project_id);

ALTER TABLE sprints
    ADD UNIQUE KEY uk_sprints_id_project (id, project_id);

ALTER TABLE tasks
    DROP FOREIGN KEY fk_tasks_sprint_id,
    DROP FOREIGN KEY fk_tasks_column_id;

ALTER TABLE tasks
    ADD COLUMN creator_id BIGINT NULL AFTER assignee_id,
    ADD COLUMN task_type VARCHAR(32) NOT NULL DEFAULT 'TASK' AFTER description,
    ADD COLUMN story_points DECIMAL(5, 2) NULL AFTER priority,
    ADD COLUMN estimated_hours DECIMAL(8, 2) NULL AFTER story_points,
    ADD COLUMN acceptance_criteria TEXT NULL AFTER due_date,
    ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE AFTER sort_order,
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER is_archived;

UPDATE tasks
JOIN projects ON projects.id = tasks.project_id
SET tasks.creator_id = projects.owner_id
WHERE tasks.creator_id IS NULL;

ALTER TABLE tasks
    MODIFY creator_id BIGINT NOT NULL,
    ADD UNIQUE KEY uk_tasks_id_project (id, project_id),
    ADD KEY idx_tasks_creator_id (creator_id),
    ADD KEY idx_tasks_sprint_project (sprint_id, project_id),
    ADD KEY idx_tasks_column_project (column_id, project_id),
    ADD CONSTRAINT fk_tasks_creator_id FOREIGN KEY (creator_id) REFERENCES users (id),
    ADD CONSTRAINT fk_tasks_sprint_project FOREIGN KEY (sprint_id, project_id) REFERENCES sprints (id, project_id),
    ADD CONSTRAINT fk_tasks_column_project FOREIGN KEY (column_id, project_id) REFERENCES board_columns (id, project_id);

ALTER TABLE task_tags
    ADD UNIQUE KEY uk_task_tags_id_project (id, project_id);

ALTER TABLE task_tag_links
    DROP FOREIGN KEY fk_task_tag_links_task_id,
    DROP FOREIGN KEY fk_task_tag_links_tag_id;

ALTER TABLE task_tag_links
    ADD COLUMN project_id BIGINT NULL AFTER tag_id;

UPDATE task_tag_links
JOIN tasks ON tasks.id = task_tag_links.task_id
SET task_tag_links.project_id = tasks.project_id
WHERE task_tag_links.project_id IS NULL;

ALTER TABLE task_tag_links
    MODIFY project_id BIGINT NOT NULL,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (task_id, tag_id, project_id),
    ADD KEY idx_task_tag_links_task_project (task_id, project_id),
    ADD KEY idx_task_tag_links_tag_project (tag_id, project_id),
    ADD CONSTRAINT fk_task_tag_links_task_project FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_task_tag_links_tag_project FOREIGN KEY (tag_id, project_id) REFERENCES task_tags (id, project_id) ON DELETE CASCADE;

ALTER TABLE task_activities
    DROP FOREIGN KEY fk_task_activities_task_id;

ALTER TABLE task_activities
    ADD COLUMN project_id BIGINT NULL AFTER task_id,
    ADD COLUMN action_type VARCHAR(60) NULL AFTER actor_id,
    ADD COLUMN field_name VARCHAR(100) NULL AFTER action_type,
    ADD COLUMN old_value TEXT NULL AFTER field_name,
    ADD COLUMN new_value TEXT NULL AFTER old_value;

UPDATE task_activities
JOIN tasks ON tasks.id = task_activities.task_id
SET task_activities.project_id = tasks.project_id,
    task_activities.action_type = task_activities.activity_type
WHERE task_activities.project_id IS NULL
   OR task_activities.action_type IS NULL;

ALTER TABLE task_activities
    MODIFY project_id BIGINT NOT NULL,
    MODIFY action_type VARCHAR(60) NOT NULL,
    ADD KEY idx_task_activities_task_project (task_id, project_id),
    ADD KEY idx_task_activities_project_id (project_id),
    ADD CONSTRAINT fk_task_activities_task_project FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE;
