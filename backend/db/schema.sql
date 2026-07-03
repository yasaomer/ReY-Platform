-- ReY Platform D1 SQLite Schema

-- 1. Authentication
CREATE TABLE IF NOT EXISTS auth_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    backup_phone TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Sessions
CREATE TABLE IF NOT EXISTS sessions (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    device_info TEXT,
    is_valid INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY(user_id) REFERENCES auth_users(id) ON DELETE CASCADE
);

-- 3. Configuration
CREATE TABLE IF NOT EXISTS configuration (
    config_key TEXT PRIMARY KEY,
    config_value TEXT NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 4. Gallery Metadata
CREATE TABLE IF NOT EXISTS gallery_metadata (
    image_id TEXT PRIMARY KEY,
    file_name TEXT NOT NULL,
    file_hash TEXT NOT NULL,
    thumbnail_id TEXT,
    width INTEGER,
    height INTEGER,
    size_bytes INTEGER,
    created_time DATETIME NOT NULL,
    modified_time DATETIME NOT NULL,
    view_count INTEGER NOT NULL DEFAULT 0,
    visibility TEXT NOT NULL DEFAULT 'public', -- public, hidden
    category TEXT,
    is_favorite INTEGER NOT NULL DEFAULT 0,
    is_hidden INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'available' -- uploading, available, deleted
);

-- 5. Gallery Statistics
CREATE TABLE IF NOT EXISTS gallery_statistics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_id TEXT NOT NULL,
    event_type TEXT NOT NULL, -- view, zoom, download, share
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    duration_seconds REAL,
    FOREIGN KEY(image_id) REFERENCES gallery_metadata(image_id) ON DELETE CASCADE
);

-- 6. Last Message
CREATE TABLE IF NOT EXISTS last_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version INTEGER NOT NULL DEFAULT 1
);

-- 7. Location Status
CREATE TABLE IF NOT EXISTS location_status (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    accuracy REAL NOT NULL,
    altitude REAL,
    speed REAL,
    heading REAL,
    address TEXT,
    provider TEXT, -- gps, network
    timestamp DATETIME NOT NULL
);

-- 8. AI Providers & Configuration
CREATE TABLE IF NOT EXISTS ai_providers (
    name TEXT PRIMARY KEY, -- gemini, openai, openrouter, deepseek
    api_key TEXT,
    base_url TEXT,
    model TEXT NOT NULL,
    temperature REAL DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2048,
    system_prompt TEXT,
    cooldown_seconds INTEGER DEFAULT 0,
    daily_limit INTEGER DEFAULT 100
);

-- 9. AI Statistics (Analytics)
CREATE TABLE IF NOT EXISTS ai_statistics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider TEXT NOT NULL,
    model TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    latency_ms INTEGER NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    status TEXT NOT NULL, -- success, failed, rate_limited, timeout
    error_message TEXT
);

-- 10. Knowledge Documents
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id TEXT PRIMARY KEY, -- document unique identifier / path hash
    title TEXT NOT NULL,
    size_bytes INTEGER NOT NULL,
    total_pages INTEGER,
    date_added DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_indexed DATETIME,
    category TEXT NOT NULL, -- personal, behavior, language, reference
    status TEXT NOT NULL DEFAULT 'indexed' -- uploading, indexing, indexed, error
);

-- 11. Documents (Raw Indexed Document Text Segments for RAG)
CREATE TABLE IF NOT EXISTS document_segments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    doc_id TEXT NOT NULL,
    segment_index INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    FOREIGN KEY(doc_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE
);

-- 12. Synchronization Queue
CREATE TABLE IF NOT EXISTS sync_queue (
    task_id TEXT PRIMARY KEY,
    task_type TEXT NOT NULL, -- upload_image, delete_image, update_message, update_location, backup
    priority INTEGER NOT NULL DEFAULT 2, -- 1 = high, 2 = normal, 3 = low
    status TEXT NOT NULL DEFAULT 'pending', -- pending, processing, completed, failed
    creation_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    owner TEXT,
    target_module TEXT NOT NULL, -- gallery, message, location, config, kb
    completion_time DATETIME,
    error_info TEXT
);

-- 13. Synchronization History
CREATE TABLE IF NOT EXISTS sync_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL,
    task_type TEXT NOT NULL,
    status TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

-- 14. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category TEXT NOT NULL, -- sync, ai, location, security, warning, error
    message TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_read INTEGER NOT NULL DEFAULT 0
);

-- 15. System Logs
CREATE TABLE IF NOT EXISTS system_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    module TEXT NOT NULL, -- auth, sync, ai, gallery, location, social, config, db
    severity TEXT NOT NULL, -- INFO, WARNING, ERROR, SECURITY
    action TEXT NOT NULL,
    message TEXT NOT NULL,
    exception TEXT,
    duration_ms INTEGER,
    user_id INTEGER,
    request_id TEXT
);

-- 16. System Backups
CREATE TABLE IF NOT EXISTS system_backups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    size_bytes INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'available',
    file_path_drive TEXT,
    payload_json TEXT NOT NULL
);

-- 17. Visitor Events
CREATE TABLE IF NOT EXISTS visitor_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    user_agent TEXT,
    platform TEXT,
    browser TEXT,
    language TEXT,
    ip_address TEXT,
    event_type TEXT NOT NULL,
    page_name TEXT,
    target_item TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    duration_seconds REAL
);
