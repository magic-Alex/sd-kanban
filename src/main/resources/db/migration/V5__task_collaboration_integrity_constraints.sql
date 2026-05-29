ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_task_requires_project CHECK (task_id IS NULL OR project_id IS NOT NULL);
