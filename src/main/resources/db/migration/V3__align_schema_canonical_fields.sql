ALTER TABLE users
    DROP INDEX uk_users_username,
    DROP COLUMN username,
    DROP COLUMN display_name;

ALTER TABLE task_activities
    DROP COLUMN activity_type;
