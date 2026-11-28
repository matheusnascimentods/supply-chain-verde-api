CREATE TABLE address (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(30) NOT NULL,
    neighborhood VARCHAR(120) NOT NULL,
    complement   VARCHAR(120) NULL,
    zip_code     VARCHAR(20) NOT NULL,
    city         VARCHAR(120) NOT NULL,
    state        VARCHAR(2) NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_address_city_state (city, state)
);
