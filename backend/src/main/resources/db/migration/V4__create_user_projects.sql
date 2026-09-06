CREATE TABLE user_projects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_projects_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_projects_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT uq_user_projects_user_project UNIQUE (user_id, project_id)
);
