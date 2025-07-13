# Shipping Quotes System Documentation

## Overview

The Shipping Quotes System is a comprehensive solution for obtaining shipping rates from multiple providers (DHL, VNPost, EasyPost, etc.) with support for both single-vendor and multi-vendor scenarios. The system includes caching, rate limiting, circuit breakers, and fallback mechanisms.

## Features

### Core Features
- **Multi-provider support** - Pluggable architecture for different shipping providers
- **Single & Multi-vendor quotes** - Support for cart with items from multiple vendors
- **Caching** - Redis-based caching with configurable TTL (default: 10 minutes)
- **Rate limiting** - API rate limiting to prevent abuse
- **Circuit breaker** - Resilience patterns for external API failures
- **Fallback calculation** - Internal rate calculation when external APIs fail
- **Analytics** - Comprehensive tracking of quotes, selections, and performance

### API Endpoints

#### Core Quote Calculation
```
POST /api/v1/shipping/quotes/calculate
POST /api/v1/shipping/quotes/cart/review
POST /api/v1/shipping/quotes/checkout/calculate
POST /api/v1/shipping/quotes/quick-estimate
```

#### Provider Management
```
GET /api/v1/shipping/quotes/providers
GET /api/v1/shipping/quotes/providers/{providerName}
GET /api/v1/shipping/quotes/providers/status
POST /api/v1/shipping/quotes/providers/test-connectivity
```

#### Validation
```
POST /api/v1/shipping/quotes/validate
```

## Request/Response Formats

### Single Vendor Request
```json
{
  "vendor": {
    "name": "Shop ABC",
    "address": {
      "country": "VN",
      "city": "Ho Chi Minh",
      "zip": "700000",
      "street": "123 ABC St"
    }
  },
  "customer": {
    "name": "Lợi Nguyễn",
    "address": {
      "country": "VN",
      "city": "Hanoi",
      "zip": "100000",
      "street": "456 DEF St"
    }
  },
  "package": {
    "weight": 1.5,
    "dimensions": {
      "length": 30,
      "width": 20,
      "height": 10
    },
    "type": "box",
    "declaredValue": 1000000,
    "currency": "VND"
  }
}
```

### Multi-Vendor Request
```json
{
  "customer": {
    "name": "Lợi Nguyễn",
    "address": {
      "country": "VN",
      "city": "Hanoi", 
      "zip": "100000",
      "street": "456 DEF St"
    }
  },
  "vendorPackages": [
    {
      "vendor": {
        "name": "Shop ABC",
        "vendorId": "vendor_001",
        "address": {
          "country": "VN",
          "city": "Ho Chi Minh",
          "zip": "700000",
          "street": "123 ABC St"
        }
      },
      "packageInfo": {
        "weight": 1.5,
        "dimensions": {
          "length": 30,
          "width": 20,
          "height": 10
        },
        "type": "box",
        "declaredValue": 1000000,
        "currency": "VND"
      }
    }
  ]
}
```

### Response Format
```json
{
  "quoteId": "SQ_1703123456789_abcd1234",
  "quotedAt": "2023-12-21T10:30:45Z",
  "expiresAt": "2023-12-21T10:40:45Z",
  "requestType": "SINGLE_VENDOR",
  "shippingOptions": [
    {
      "provider": "DHL",
      "service": "Express",
      "cost": 100000,
      "currency": "VND",
      "estimatedDays": 2,
      "estimatedDeliveryDate": "2023-12-23",
      "trackingSupported": "Yes",
      "insuranceIncluded": true,
      "features": ["Tracking", "Insurance", "Signature required"]
    },
    {
      "provider": "VNPost",
      "service": "Economy", 
      "cost": 30000,
      "currency": "VND",
      "estimatedDays": 5,
      "estimatedDeliveryDate": "2023-12-26",
      "trackingSupported": "Yes",
      "insuranceIncluded": true,
      "features": ["Tracking", "Insurance", "COD available"]
    }
  ],
  "recommendedOption": { /* best balanced option */ },
  "cheapestOption": { /* lowest cost option */ },
  "fastestOption": { /* quickest delivery option */ },
  "metadata": {
    "originCountry": "VN",
    "destinationCountry": "VN", 
    "isDomestic": true,
    "totalPackages": 1,
    "calculationMethod": "EXTERNAL_API"
  }
}
```

## Architecture

### Provider Abstraction Layer

The system uses a `ShippingProviderClient` interface that can be implemented for different providers:

```java
public interface ShippingProviderClient {
    List<ShippingOption> getShippingQuotes(ShippingQuoteRequest request);
    Map<String, List<ShippingOption>> getMultiVendorQuotes(ShippingQuoteRequest request);
    boolean supportsRoute(String originCountry, String destinationCountry);
    boolean isAvailable();
    // ... other methods
}
```

### Implemented Providers

1. **EasyPostShippingClient** - Aggregates multiple carriers (DHL, FedEx, UPS, etc.)
2. **VNPostShippingClient** - Vietnam Post with domestic and international capabilities
3. **Internal fallback** - Simple weight/distance-based calculation

### Configuration

The system is configured via `application-shipping-quotes.yaml`:

```yaml
shipping:
  cache:
    ttl-minutes: 10
  rate-limit:
    requests-per-minute: 60
  providers:
    easypost:
      enabled: false
      api-key: ${EASYPOST_API_KEY:}
    vnpost:
      enabled: true
      api-key: ${VNPOST_API_KEY:}
```

## Database Schema

### Shipping Quotes Table
```sql
CREATE TABLE shipping_quotes (
    quote_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    external_quote_id VARCHAR(100),
    customer_id BIGINT NOT NULL,
    vendor_id VARCHAR(50),
    origin_country VARCHAR(3) NOT NULL,
    destination_country VARCHAR(3) NOT NULL,
    total_weight DECIMAL(8,2) NOT NULL,
    total_value DECIMAL(10,2) NOT NULL,
    best_option_provider VARCHAR(50),
    best_option_cost DECIMAL(10,2),
    cache_hit BOOLEAN DEFAULT FALSE,
    processing_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ... other fields
);
```

## Caching Strategy

- **Redis-based caching** with 10-minute TTL
- **Cache key** based on route, weight, value, and customer
- **Smart cache invalidation** for expired quotes
- **Cache hit tracking** for analytics

## Rate Limiting

- **60 requests per minute** per client (configurable)
- **Burst capacity** of 10 requests
- **Client identification** via API key or IP address
- **Rate limit headers** in responses

## Resilience Patterns

### Circuit Breaker
- **50% failure rate threshold** opens circuit
- **30-second wait** in open state
- **Automatic recovery** via half-open state

### Retry Logic
- **3 retry attempts** with exponential backoff
- **Retries on** network errors, timeouts, and service unavailable
- **Different strategies** per provider

### Fallback
- **Internal calculation** when all providers fail
- **Configurable rates** based on weight and distance
- **Graceful degradation** with clear indication

## Security

### API Key Management
- **Environment variables** for sensitive keys
- **Vault integration** ready for production
- **Provider-specific** key management

### Input Validation
- **Jakarta Validation** annotations
- **Custom validators** for shipping-specific rules
- **Request sanitization** to prevent injection

## Monitoring & Analytics

### Metrics Tracked
- Quote volume by provider, route, time
- Provider performance (response time, success rate)
- Cache hit ratios
- Quote-to-shipment conversion rates
- Processing times by provider

### Views and Reports
```sql
-- Analytics view
CREATE VIEW shipping_quote_analytics AS ...

-- Provider performance view  
CREATE VIEW provider_performance AS ...
```

## Usage Examples

### Cart Review (with caching)
```bash
curl -X POST /api/v1/shipping/quotes/cart/review \
  -H "Content-Type: application/json" \
  -d '{ "vendor": {...}, "customer": {...}, "package": {...} }'
```

### Checkout (fresh calculation)
```bash
curl -X POST /api/v1/shipping/quotes/checkout/calculate \
  -H "Content-Type: application/json" \
  -d '{ "vendor": {...}, "customer": {...}, "package": {...} }'
```

### Multi-vendor Cart
```bash
curl -X POST /api/v1/shipping/quotes/calculate \
  -H "Content-Type: application/json" \
  -d '{ "customer": {...}, "vendorPackages": [...] }'
```

### Provider Status
```bash
curl -X GET /api/v1/shipping/quotes/providers/status
```

## Performance Considerations

- **Parallel provider queries** (max 5 concurrent)
- **30-second timeout** per provider
- **Response compression** for large quote responses
- **Database indexing** on frequently queried fields
- **Connection pooling** for Redis and databases

## Error Handling

### Provider Errors
- **Graceful degradation** when providers fail
- **Error tracking** and analytics
- **Fallback to internal calculation**

### Validation Errors
- **Detailed error messages** for invalid requests
- **Field-level validation** results
- **Suggestion messages** for common issues

## Deployment

### Environment Variables
```bash
# Provider API keys
EASYPOST_API_KEY=your_easypost_key
VNPOST_API_KEY=your_vnpost_key

# Redis configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# Rate limiting
SHIPPING_RATE_LIMIT_ENABLED=true
SHIPPING_RATE_LIMIT_REQUESTS_PER_MINUTE=60
```

### Docker Configuration
```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  shipping-service:
    image: shipping-service:latest
    environment:
      - REDIS_HOST=redis
      - EASYPOST_API_KEY=${EASYPOST_API_KEY}
    depends_on:
      - redis
```

## Testing

### Unit Tests
- Provider client mocking
- Service layer testing
- Validation testing

### Integration Tests
- End-to-end API testing
- Provider connectivity testing
- Cache behavior testing
- Rate limiting testing

### Load Testing
- Concurrent quote requests
- Provider failure scenarios
- Cache performance under load

## Future Enhancements

### Additional Providers
- **Giao Hang Nhanh (GHN)** - Vietnamese logistics
- **DHL Express API** - Direct integration
- **FedEx API** - Direct integration
- **Local delivery services**

### Advanced Features
- **Real-time tracking** integration
- **Shipping insurance** calculation
- **Customs documentation** generation
- **Pickup scheduling** coordination
- **Machine learning** for rate optimization

### Performance Optimizations
- **GraphQL** for flexible data fetching
- **Streaming responses** for large quote sets
- **Predictive caching** based on patterns
- **Edge caching** for common routes

## Support

For issues or questions about the Shipping Quotes System:

1. Check the logs in `/logs/shipping-quotes.log`
2. Monitor provider status via `/api/v1/shipping/quotes/providers/status`
3. Review cache performance in Redis
4. Check database analytics views for patterns

## API Documentation

Full API documentation is available via Swagger UI at:
`/swagger-ui.html#/shipping-quote-controller` 
