-- =============================================
-- SHIPPING SERVICE DATABASE SCHEMA
-- =============================================

-- Create database
CREATE DATABASE shipping_service;
USE shipping_service;

-- Shipping carriers table
CREATE TABLE shipping_carriers (
    carrier_id INT PRIMARY KEY AUTO_INCREMENT,
    carrier_name VARCHAR(100) UNIQUE NOT NULL,
    carrier_code VARCHAR(20) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    supported_countries JSON,
    api_endpoint VARCHAR(255),
    configuration JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_carrier_code (carrier_code),
    INDEX idx_is_active (is_active)
);

-- Shipping zones table
CREATE TABLE shipping_zones (
    zone_id INT PRIMARY KEY AUTO_INCREMENT,
    zone_name VARCHAR(100) NOT NULL,
    zone_code VARCHAR(20) UNIQUE NOT NULL,
    countries JSON NOT NULL,
    states_provinces JSON,
    zip_codes JSON,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_zone_code (zone_code),
    INDEX idx_is_active (is_active)
);

-- Shipping methods table
CREATE TABLE shipping_methods (
    method_id INT PRIMARY KEY AUTO_INCREMENT,
    carrier_id INT NOT NULL,
    zone_id INT NOT NULL,
    method_name VARCHAR(100) NOT NULL,
    method_code VARCHAR(50) NOT NULL,
    service_type ENUM('standard', 'express', 'overnight', 'same_day', 'economy') NOT NULL,
    base_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    per_kg_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    per_item_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    min_weight DECIMAL(8,2) DEFAULT 0.00,
    max_weight DECIMAL(8,2) DEFAULT 999.99,
    min_order_value DECIMAL(10,2) DEFAULT 0.00,
    max_order_value DECIMAL(10,2) DEFAULT 999999.99,
    estimated_days_min INT DEFAULT 1,
    estimated_days_max INT DEFAULT 7,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (carrier_id) REFERENCES shipping_carriers(carrier_id) ON DELETE CASCADE,
    FOREIGN KEY (zone_id) REFERENCES shipping_zones(zone_id) ON DELETE CASCADE,
    INDEX idx_carrier_id (carrier_id),
    INDEX idx_zone_id (zone_id),
    INDEX idx_method_code (method_code),
    INDEX idx_service_type (service_type),
    INDEX idx_is_active (is_active)
);

-- Shipments table
CREATE TABLE shipments (
    shipment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    shipment_number VARCHAR(50) UNIQUE NOT NULL,
    carrier_id INT NOT NULL,
    method_id INT NOT NULL,
    tracking_number VARCHAR(100),
    shipping_label_url VARCHAR(500),
    from_address JSON NOT NULL,
    to_address JSON NOT NULL,
    package_count INT DEFAULT 1,
    total_weight DECIMAL(8,2) NOT NULL,
    total_value DECIMAL(10,2) NOT NULL,
    shipping_cost DECIMAL(10,2) NOT NULL,
    insurance_cost DECIMAL(10,2) DEFAULT 0.00,
    status ENUM('pending', 'label_created', 'picked_up', 'in_transit', 'out_for_delivery', 'delivered', 'failed', 'cancelled', 'returned') DEFAULT 'pending',
    shipped_at TIMESTAMP NULL,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    delivery_signature VARCHAR(255),
    delivery_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (carrier_id) REFERENCES shipping_carriers(carrier_id),
    FOREIGN KEY (method_id) REFERENCES shipping_methods(method_id),
    INDEX idx_order_id (order_id),
    INDEX idx_shipment_number (shipment_number),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_carrier_id (carrier_id),
    INDEX idx_status (status),
    INDEX idx_shipped_at (shipped_at),
    INDEX idx_estimated_delivery_date (estimated_delivery_date)
);

-- Shipment items table
CREATE TABLE shipment_items (
    shipment_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_weight DECIMAL(8,2) NOT NULL,
    total_weight DECIMAL(8,2) NOT NULL,
    dimensions JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_order_item_id (order_item_id),
    INDEX idx_product_id (product_id)
);

-- Shipment packages table
CREATE TABLE shipment_packages (
    package_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    package_number VARCHAR(50) NOT NULL,
    tracking_number VARCHAR(100),
    weight DECIMAL(8,2) NOT NULL,
    dimensions JSON NOT NULL,
    package_type VARCHAR(50) DEFAULT 'box',
    is_fragile BOOLEAN DEFAULT FALSE,
    is_liquid BOOLEAN DEFAULT FALSE,
    is_hazardous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_package_number (package_number),
    INDEX idx_tracking_number (tracking_number)
);

-- Shipment tracking events table
CREATE TABLE shipment_tracking_events (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT,
    event_location VARCHAR(255),
    event_timestamp TIMESTAMP NOT NULL,
    carrier_event_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_event_type (event_type),
    INDEX idx_event_timestamp (event_timestamp)
);

-- Shipping rate calculations table
CREATE TABLE shipping_rate_calculations (
    calculation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT,
    from_zip VARCHAR(20) NOT NULL,
    to_zip VARCHAR(20) NOT NULL,
    total_weight DECIMAL(8,2) NOT NULL,
    total_value DECIMAL(10,2) NOT NULL,
    package_count INT DEFAULT 1,
    requested_service_type VARCHAR(50),
    calculated_rates JSON NOT NULL,
    selected_method_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_order_id (order_id),
    INDEX idx_from_zip (from_zip),
    INDEX idx_to_zip (to_zip),
    INDEX idx_created_at (created_at)
);

-- Shipping webhooks table
CREATE TABLE shipping_webhooks (
    webhook_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT,
    carrier_id INT NOT NULL,
    webhook_type VARCHAR(50) NOT NULL,
    tracking_number VARCHAR(100),
    webhook_data JSON NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_carrier_id (carrier_id),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_webhook_type (webhook_type),
    INDEX idx_processed (processed),
    INDEX idx_received_at (received_at)
);

-- =============================================
-- SAMPLE DATA FOR SHIPPING SERVICE
-- =============================================

-- Sample shipping carriers
INSERT INTO shipping_carriers (carrier_name, carrier_code, is_active, supported_countries, api_endpoint, configuration) VALUES
('UPS', 'UPS', TRUE, '["US", "CA", "MX", "GB", "DE", "FR"]', 'https://api.ups.com/v1/', '{"api_key": "test_key", "account_number": "123456"}'),
('FedEx', 'FEDEX', TRUE, '["US", "CA", "MX", "GB", "DE", "FR", "AU"]', 'https://api.fedex.com/v1/', '{"api_key": "test_key", "account_number": "789012"}'),
('DHL', 'DHL', TRUE, '["US", "CA", "MX", "GB", "DE", "FR", "AU", "IN"]', 'https://api.dhl.com/v1/', '{"api_key": "test_key", "account_number": "345678"}'),
('USPS', 'USPS', TRUE, '["US"]', 'https://api.usps.com/v1/', '{"api_key": "test_key", "user_id": "test_user"}');

-- Sample shipping zones
INSERT INTO shipping_zones (zone_name, zone_code, countries, states_provinces, is_active) VALUES
('US Domestic', 'US_DOM', '["US"]', '["AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"]', TRUE),
('Canada', 'CA', '["CA"]', '["AB", "BC", "MB", "NB", "NL", "NT", "NS", "NU", "ON", "PE", "QC", "SK", "YT"]', TRUE),
('Europe', 'EU', '["GB", "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH", "SE", "NO", "DK", "FI"]', NULL, TRUE),
('International', 'INTL', '["AU", "IN", "JP", "CN", "MX", "BR"]', NULL, TRUE);

-- Sample shipping methods
INSERT INTO shipping_methods (carrier_id, zone_id, method_name, method_code, service_type, base_rate, per_kg_rate, per_item_rate, min_weight, max_weight, min_order_value, estimated_days_min, estimated_days_max, is_active) VALUES
(1, 1, 'UPS Ground', 'UPS_GROUND', 'standard', 8.99, 2.50, 0.00, 0.00, 70.00, 0.00, 3, 7, TRUE),
(1, 1, 'UPS Next Day Air', 'UPS_NEXT_DAY', 'overnight', 25.99, 5.00, 0.00, 0.00, 70.00, 0.00, 1, 1, TRUE),
(1, 1, 'UPS 2nd Day Air', 'UPS_2DAY', 'express', 18.99, 3.50, 0.00, 0.00, 70.00, 0.00, 2, 2, TRUE),
(2, 1, 'FedEx Ground', 'FEDEX_GROUND', 'standard', 9.99, 2.75, 0.00, 0.00, 68.00, 0.00, 3, 7, TRUE),
(2, 1, 'FedEx Priority Overnight', 'FEDEX_PRIORITY', 'overnight', 29.99, 5.50, 0.00, 0.00, 68.00, 0.00, 1, 1, TRUE),
(4, 1, 'USPS Priority Mail', 'USPS_PRIORITY', 'standard', 7.99, 1.50, 0.00, 0.00, 31.75, 0.00, 2, 5, TRUE),
(4, 1, 'USPS Priority Mail Express', 'USPS_EXPRESS', 'express', 22