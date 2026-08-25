-- CREATE DATABASE IF NOT EXISTS eval
-- USE eval;
-- CREATE INDEX idx_evaluation_status ON evaluation_cases(evaluation_status);
-- CREATE INDEX idx_eval_status_confidence ON evaluation_cases(evaluation_status, ai_confidence);

CREATE TABLE IF NOT EXISTS reference_cases (
    reference_case_id VARCHAR(100) NOT NULL PRIMARY KEY,
    target_title VARCHAR(255) NOT NULL,
    source_title VARCHAR(255) NOT NULL,
    human_label VARCHAR(20) NOT NULL,
    human_reason TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL
);


CREATE TABLE IF NOT EXISTS evaluation_cases (
    evaluation_case_id VARCHAR(100) NOT NULL PRIMARY KEY,
    target_title VARCHAR(255) NOT NULL,
    source_title VARCHAR(255) NOT NULL,
    ai_label VARCHAR(20) DEFAULT NULL,
    ai_confidence DOUBLE DEFAULT NULL,
    evaluation_status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    human_label VARCHAR(20) DEFAULT NULL,
    human_reason TEXT DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    evaluated_at DATETIME(6) DEFAULT NULL,
    reviewed_at DATETIME(6) DEFAULT NULL,
    retrieved_info JSON DEFAULT NULL
);
