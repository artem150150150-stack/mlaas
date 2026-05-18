CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_owner ON projects(owner_id);

CREATE TABLE datasets (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    storage_uri TEXT NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    target_column VARCHAR(255) NOT NULL,
    feature_columns_json TEXT NOT NULL,
    row_count BIGINT,
    column_stats_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_datasets_project ON datasets(project_id);

CREATE TABLE ml_models (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    dataset_id UUID NOT NULL REFERENCES datasets(id),
    name VARCHAR(200) NOT NULL,
    model_kind VARCHAR(64) NOT NULL,
    artifact_uri TEXT,
    latest_training_task_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ml_models_project ON ml_models(project_id);

CREATE TABLE training_tasks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    dataset_id UUID NOT NULL REFERENCES datasets(id),
    ml_model_id UUID REFERENCES ml_models(id),
    model_kind VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    hyperparameters_json TEXT NOT NULL DEFAULT '{}',
    error_message TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_status ON training_tasks(project_id, status);

CREATE TABLE training_metrics (
    id UUID PRIMARY KEY,
    training_task_id UUID NOT NULL UNIQUE REFERENCES training_tasks(id) ON DELETE CASCADE,
    accuracy DOUBLE PRECISION,
    precision_macro DOUBLE PRECISION,
    recall_macro DOUBLE PRECISION,
    f1_macro DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    confusion_matrix_json TEXT,
    train_score DOUBLE PRECISION,
    val_score DOUBLE PRECISION,
    overfitting_estimate DOUBLE PRECISION
);

CREATE TABLE explainability_reports (
    id UUID PRIMARY KEY,
    training_task_id UUID NOT NULL REFERENCES training_tasks(id) ON DELETE CASCADE,
    feature_importance_json TEXT NOT NULL,
    shap_values_json TEXT,
    shap_plot_uri TEXT,
    lime_explanations_json TEXT,
    fairness_metrics_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_explain_task ON explainability_reports(training_task_id);

CREATE TABLE recommendations (
    id UUID PRIMARY KEY,
    training_task_id UUID NOT NULL REFERENCES training_tasks(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    message TEXT NOT NULL,
    details_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reco_task ON recommendations(training_task_id);

CREATE TABLE metric_snapshots (
    id UUID PRIMARY KEY,
    training_task_id UUID NOT NULL REFERENCES training_tasks(id) ON DELETE CASCADE,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metric_name VARCHAR(64) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    drift_score DOUBLE PRECISION,
    simulated BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_snapshots_task_time ON metric_snapshots(training_task_id, captured_at);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64),
    entity_id UUID,
    ip_address VARCHAR(45),
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
