-- Add shipping quotes table for storing external provider quotes
CREATE TABLE shipping_quotes (
    quote_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    external_quote_id VARCHAR(100),
    customer_id BIGINT NOT NULL,
    vendor_id VARCHAR(50),
    vendor_name VARCHAR(255),
    request_type VARCHAR(20) NOT NULL,
    origin_country VARCHAR(3) NOT NULL,
    destination_country VARCHAR(3) NOT NULL,
    total_weight DECIMAL(8,2) NOT NULL,
    total_value DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    package_count INT NOT NULL DEFAULT 1,
    is_domestic BOOLEAN NOT NULL DEFAULT FALSE,
    requires_customs BOOLEAN NOT NULL DEFAULT FALSE,
    request_data JSON,
    response_data JSON,
    best_option_provider VARCHAR(50),
    best_option_service VARCHAR(100),
    best_option_cost DECIMAL(10,2),
    best_option_currency VARCHAR(3),
    best_option_delivery_days INT,
    cheapest_option_provider VARCHAR(50),
    cheapest_option_cost DECIMAL(10,2),
    fastest_option_provider VARCHAR(50),
    fastest_option_delivery_days INT,
    total_options_count INT NOT NULL DEFAULT 0,
    providers_queried JSON,
    providers_responded JSON,
    provider_errors JSON,
    calculation_method VARCHAR(50),
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    processing_time_ms BIGINT,
    expires_at TIMESTAMP,
    selected_option_id VARCHAR(100),
    selected_provider VARCHAR(50),
    selected_service VARCHAR(100),
    selected_cost DECIMAL(10,2),
    selection_timestamp TIMESTAMP,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    related_shipment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_shipping_quotes_shipment 
        FOREIGN KEY (related_shipment_id) 
        REFERENCES shipments(shipment_id) 
        ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_shipping_quotes_customer (customer_id),
    INDEX idx_shipping_quotes_external_id (external_quote_id),
    INDEX idx_shipping_quotes_vendor (vendor_id),
    INDEX idx_shipping_quotes_route (origin_country, destination_country),
    INDEX idx_shipping_quotes_created_at (created_at),
    INDEX idx_shipping_quotes_expires_at (expires_at),
    INDEX idx_shipping_quotes_selected_provider (selected_provider),
    INDEX idx_shipping_quotes_is_used (is_used),
    INDEX idx_shipping_quotes_cache_hit (cache_hit),
    INDEX idx_shipping_quotes_customer_created (customer_id, created_at),
    INDEX idx_shipping_quotes_route_domestic (origin_country, destination_country, is_domestic)
);

-- Add indexes to existing tables for better performance with shipping quotes
ALTER TABLE shipments 
ADD INDEX idx_shipments_order_customer (order_id),
ADD INDEX idx_shipments_tracking_number (tracking_number),
ADD INDEX idx_shipments_status_created (status, created_at);

-- Add provider tracking to existing rate calculations table if not exists
ALTER TABLE shipping_rate_calculations 
ADD COLUMN IF NOT EXISTS provider_used VARCHAR(50),
ADD COLUMN IF NOT EXISTS external_quote_id VARCHAR(100),
ADD INDEX IF NOT EXISTS idx_rate_calc_provider (provider_used),
ADD INDEX IF NOT EXISTS idx_rate_calc_external_id (external_quote_id);

-- Create a view for shipping quote analytics
CREATE OR REPLACE VIEW shipping_quote_analytics AS
SELECT 
    DATE(created_at) as quote_date,
    origin_country,
    destination_country,
    is_domestic,
    COUNT(*) as total_quotes,
    COUNT(CASE WHEN cache_hit = TRUE THEN 1 END) as cache_hits,
    COUNT(CASE WHEN is_used = TRUE THEN 1 END) as quotes_used,
    COUNT(CASE WHEN selected_provider IS NOT NULL THEN 1 END) as quotes_selected,
    AVG(processing_time_ms) as avg_processing_time_ms,
    AVG(total_options_count) as avg_options_count,
    AVG(best_option_cost) as avg_best_cost,
    MIN(best_option_cost) as min_best_cost,
    MAX(best_option_cost) as max_best_cost
FROM shipping_quotes
WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
GROUP BY DATE(created_at), origin_country, destination_country, is_domestic
ORDER BY quote_date DESC;

-- Create a view for provider performance
CREATE OR REPLACE VIEW provider_performance AS
SELECT 
    selected_provider as provider,
    COUNT(*) as total_selections,
    AVG(selected_cost) as avg_cost,
    AVG(best_option_delivery_days) as avg_delivery_days,
    COUNT(CASE WHEN is_used = TRUE THEN 1 END) as actual_shipments,
    (COUNT(CASE WHEN is_used = TRUE THEN 1 END) * 100.0 / COUNT(*)) as conversion_rate
FROM shipping_quotes
WHERE selected_provider IS NOT NULL
  AND created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
GROUP BY selected_provider
ORDER BY total_selections DESC;

-- Add comments for documentation
ALTER TABLE shipping_quotes 
COMMENT = 'Stores shipping quotes from external providers for analytics and caching';

-- Create cleanup procedure for expired quotes
DELIMITER //
CREATE PROCEDURE CleanupExpiredQuotes()
BEGIN
    DECLARE rows_deleted INT DEFAULT 0;
    
    -- Delete expired quotes that are older than 7 days and not used
    DELETE FROM shipping_quotes 
    WHERE expires_at IS NOT NULL 
      AND expires_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
      AND is_used = FALSE
      AND related_shipment_id IS NULL;
    
    SET rows_deleted = ROW_COUNT();
    
    -- Log the cleanup
    INSERT INTO system_logs (log_level, message, created_at) 
    VALUES ('INFO', CONCAT('Cleaned up ', rows_deleted, ' expired shipping quotes'), NOW());
    
END //
DELIMITER ;

-- Create event to run cleanup daily
CREATE EVENT IF NOT EXISTS evt_cleanup_expired_quotes
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
  CALL CleanupExpiredQuotes(); 
