# Shipping Service

A comprehensive microservice for managing shipping operations, including carriers, zones, methods, shipments, and tracking.

## Architecture Overview

This service follows a clean architecture pattern with comprehensive JPA entities and repositories for all shipping-related operations.

## Database Schema

The service manages the following main entities:

### Core Entities

1. **Shipping Carriers** (`EShippingCarrier`)
   - Manages shipping providers (UPS, FedEx, DHL, USPS)
   - Stores carrier configuration and supported countries
   - Tracks API endpoints and credentials

2. **Shipping Zones** (`EShippingZone`)
   - Defines geographic shipping zones
   - Supports country, state/province, and ZIP code restrictions
   - Enables zone-based shipping rules

3. **Shipping Methods** (`EShippingMethod`)
   - Links carriers to zones with specific service types
   - Defines pricing structure (base rate, per-kg rate, per-item rate)
   - Sets weight and order value constraints
   - Specifies delivery time estimates

### Shipment Management

4. **Shipments** (`EShipment`)
   - Core shipment entity with tracking information
   - Links to carriers and shipping methods
   - Stores addresses, weight, value, and costs
   - Tracks delivery status and signatures

5. **Shipment Items** (`EShipmentItem`)
   - Individual items within a shipment
   - Links to product information and quantities
   - Tracks individual item weights and dimensions

6. **Shipment Packages** (`EShipmentPackage`)
   - Physical packages within a shipment
   - Tracks package dimensions and special handling requirements
   - Supports fragile, liquid, and hazardous materials

### Tracking and Operations

7. **Shipment Tracking Events** (`EShipmentTrackingEvent`)
   - Comprehensive tracking history
   - Stores carrier events and locations
   - Maintains event timestamps and descriptions

8. **Shipping Rate Calculations** (`EShippingRateCalculation`)
   - Rate calculation history and caching
   - Stores calculation parameters and results
   - Enables rate comparison and optimization

9. **Shipping Webhooks** (`EShippingWebhook`)
   - Carrier webhook management
   - Tracks webhook processing status
   - Maintains webhook data and timestamps

## Repository Layer

Each entity has a corresponding repository with comprehensive query methods:

### Repository Features

- **CRUD Operations**: Standard JPA repository methods
- **Custom Queries**: Business-specific finder methods
- **Pagination Support**: Page-based queries for large datasets
- **Aggregation Methods**: Count, sum, and statistical queries
- **Status Tracking**: Active/inactive and processing status queries
- **Date Range Queries**: Time-based filtering capabilities

### Key Repository Interfaces

- `ShippingCarrierRepository`: Carrier management and country support queries
- `ShippingZoneRepository`: Zone-based shipping rule queries
- `ShippingMethodRepository`: Rate calculation and service type queries
- `ShipmentRepository`: Comprehensive shipment tracking and status queries
- `ShipmentTrackingEventRepository`: Event history and timeline queries
- `ShippingWebhookRepository`: Webhook processing and monitoring queries

## Service Types

The system supports the following shipping service types:

- **STANDARD**: Regular ground shipping
- **EXPRESS**: Expedited shipping (2-3 days)
- **OVERNIGHT**: Next-day delivery
- **SAME_DAY**: Same-day delivery
- **ECONOMY**: Budget shipping option

## Shipment Status Tracking

Shipments progress through the following statuses:

- **PENDING**: Initial state
- **LABEL_CREATED**: Shipping label generated
- **PICKED_UP**: Package picked up by carrier
- **IN_TRANSIT**: Package in transit
- **OUT_FOR_DELIVERY**: Package out for delivery
- **DELIVERED**: Package delivered
- **FAILED**: Delivery failed
- **CANCELLED**: Shipment cancelled
- **RETURNED**: Package returned to sender

## Key Features

### Carrier Integration
- Multi-carrier support with unified API
- Carrier-specific configuration management
- Rate comparison across carriers

### Zone-Based Shipping
- Flexible geographic zone definitions
- Zone-specific pricing and service levels
- Country and ZIP code restrictions

### Package Management
- Multi-package shipment support
- Special handling for fragile/hazardous items
- Individual package tracking

### Comprehensive Tracking
- Real-time tracking event processing
- Webhook-based carrier updates
- Historical tracking data

### Rate Calculation
- Dynamic rate calculation based on zones and methods
- Rate caching and optimization
- Service type and delivery time comparisons

## Entity Relationships

```
EShippingCarrier 1--* EShippingMethod
EShippingZone 1--* EShippingMethod
EShippingMethod 1--* EShipment
EShipment 1--* EShipmentItem
EShipment 1--* EShipmentPackage
EShipment 1--* EShipmentTrackingEvent
EShippingCarrier 1--* EShippingWebhook
EShipment 1--* EShippingWebhook
```

## Configuration

The service uses Spring Boot with JPA/Hibernate for database operations. All entities use proper JPA annotations with:

- Automatic ID generation
- Proper foreign key relationships
- Lazy loading for performance
- Timestamp auditing
- JSON column support for flexible data storage

## Getting Started

1. Configure your database connection in `application.yaml`
2. Run the application - tables will be created automatically
3. Use the repository injection to access data operations
4. Refer to the SQL schema in `shipping_service_sql.sql` for sample data

## Dependencies

- Spring Boot
- Spring Data JPA
- Hibernate
- Lombok
- Jackson (for JSON processing)

This architecture provides a solid foundation for a comprehensive shipping management system with full CRUD operations, complex queries, and extensible design patterns. 