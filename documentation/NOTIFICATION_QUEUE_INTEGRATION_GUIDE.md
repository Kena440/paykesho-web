# Notification Queue Integration Guide

## Overview

This guide explains how the new notification queue system has been integrated with the existing notification structure in the Paykesho web application. The integration provides a seamless bridge between the old and new systems while maintaining backward compatibility.

## Architecture

### Components Added

1. **NotificationQueue Entity** - Enhanced notification entity with queue capabilities
2. **NotificationStatus Enum** - Comprehensive status tracking
3. **NotificationQueueDto** - Data transfer object
4. **NotificationQueueRepository** - Repository with advanced queries
5. **NotificationQueueService** - Business logic service
6. **NotificationProcessorService** - Scheduled processing service
7. **EnhancedNotificationService** - Integration service
8. **NotificationQueueController** - REST API controller
9. **NotificationAsyncConfig** - Async configuration

### Integration Strategy

The integration follows a **harmonization approach** that:

- ✅ Maintains backward compatibility with existing `Notification` entity
- ✅ Creates parallel `NotificationQueue` entity for enhanced functionality
- ✅ Provides adapter pattern to bridge old and new systems
- ✅ Offers both immediate and queued notification sending
- ✅ Includes comprehensive status tracking and retry mechanisms

## Key Features

### 1. Dual Entity System

**Existing Notification Entity:**
```java
@Entity
@Table(name = "notifications")
public class Notification extends IdBasedEntity {
    private String reference;
    private String recipient;
    private String message;
    private String status; // "pending", "sent", "failed"
    private NotificationType notificationType;
    private Date createdAt;
}
```

**New NotificationQueue Entity:**
```java
@Entity
@Table(name = "notification_queue")
public class NotificationQueue extends IdBasedEntity {
    private NotificationStatus status; // PENDING, PROCESSING, SENT, FAILED, etc.
    private String recipientEmail;
    private String subject;
    private String content;
    private Integer priority;
    private Integer retryCount;
    private Integer maxRetries;
    private Date scheduledFor;
    private String templateName;
    private String metadata;
    // ... and more advanced fields
}
```

### 2. Status Mapping

| Queue Status | Legacy Status | Description |
|---------------|----------------|-------------|
| PENDING | "pending" | Queued for processing |
| PROCESSING | "processing" | Currently being sent |
| SENT | "sent" | Successfully delivered |
| FAILED | "failed" | Failed but can retry |
| FAILED_PERMANENTLY | "failed" | Failed permanently |
| CANCELLED | "cancelled" | User cancelled |

### 3. Service Integration

**NotificationService** now provides both approaches in a single unified service:

```java
// New queued approach (recommended)
public NotificationQueue sendNotificationQueued(String recipient, String subject, String message, String notificationType)

// Legacy immediate approach (still functional)
public boolean sendNotification(String recipient, String message, String notificationType)
```

## Usage Examples

### 1. Basic Notification Sending

**Old Way (Still Works):**
```java
@Autowired
private NotificationService notificationService;

boolean success = notificationService.sendNotification("user@example.com", "Hello World", "email");
```

**New Way (Recommended):**
```java
@Autowired
private NotificationService notificationService;

NotificationQueue notification = notificationService.sendNotificationQueued(
    "user@example.com", 
    "Hello World", 
    "Welcome to our platform!", 
    "email"
);
```

### 2. High Priority Notifications

```java
NotificationQueue urgentNotification = notificationService.sendHighPriorityNotification(
    "admin@example.com",
    "System Alert",
    "Critical system issue detected",
    "email"
);
```

### 3. Scheduled Notifications

```java
Date scheduledTime = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now

NotificationQueue scheduledNotification = notificationService.sendScheduledNotification(
    "user@example.com",
    "Reminder",
    "Don't forget your appointment",
    "email",
    scheduledTime
);
```

### 4. Notifications with Metadata

```java
NotificationQueue notification = notificationService.sendNotificationWithMetadata(
    "user@example.com",
    "Payment Confirmation",
    "Your payment has been processed",
    "email",
    "{\"paymentId\":\"12345\",\"amount\":\"100.00\"}",
    "PAYMENT_12345",
    "PAYMENT"
);
```

## API Endpoints

### Queue Management

```bash
# Add notification to queue
POST /api/notifications/queue/add
?notificationType=EMAIL&recipientEmail=user@example.com&subject=Hello&content=World

# Add high priority notification
POST /api/notifications/queue/add-high-priority
?notificationType=EMAIL&recipientEmail=admin@example.com&subject=Alert&content=Critical

# Add scheduled notification
POST /api/notifications/queue/add-scheduled
?notificationType=EMAIL&recipientEmail=user@example.com&subject=Reminder&content=Don't forget&scheduledForTimestamp=1640995200000

# Get notifications by status
GET /api/notifications/queue/status/PENDING

# Get notifications by recipient
GET /api/notifications/queue/recipient/user@example.com

# Cancel notification
POST /api/notifications/queue/{id}/cancel

# Retry failed notification
POST /api/notifications/queue/{id}/retry

# Get queue statistics
GET /api/notifications/queue/statistics

# Get queue health
GET /api/notifications/queue/health
```

## Configuration

### Async Processing

The system uses configurable thread pools:

```java
@Configuration
@EnableAsync
@EnableScheduling
public class NotificationAsyncConfig {
    
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        // Email sending thread pool
    }
    
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        // Notification processing thread pool
    }
}
```

### Scheduled Processing

- **Pending notifications**: Processed every 30 seconds
- **Retryable notifications**: Processed every 5 minutes
- **Stale notifications**: Reset every 10 minutes
- **Cleanup**: Daily at 2 AM

## Migration Strategy

### Phase 1: Parallel Operation (Current)
- Both old and new systems work simultaneously
- New features use queue system
- Existing code continues to work unchanged

### Phase 2: Gradual Migration
- Update existing services to use `EnhancedNotificationService`
- Replace direct `NotificationService` calls with queued versions
- Monitor performance and reliability

### Phase 3: Full Migration (Future)
- Deprecate old `NotificationService` methods
- Remove legacy notification creation
- Use only queue-based system

## Benefits of the Queue System

### 1. **Reliability**
- Automatic retry mechanisms
- Failure tracking and reporting
- Stale notification recovery

### 2. **Performance**
- Asynchronous processing
- Rate limiting and batching
- Priority-based processing

### 3. **Monitoring**
- Comprehensive statistics
- Health status reporting
- Processing metrics

### 4. **Flexibility**
- Scheduled notifications
- Template support
- Metadata tracking
- Priority levels

### 5. **Scalability**
- Configurable thread pools
- Batch processing
- Rate limiting

## Database Schema

### New Tables Created

```sql
-- Notification queue table
CREATE TABLE notification_queue (
    id INT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP,
    notification_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(128),
    subject VARCHAR(255),
    content TEXT,
    template_name VARCHAR(100),
    template_variables TEXT,
    priority INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    scheduled_for TIMESTAMP,
    sent_at TIMESTAMP,
    processing_started_at TIMESTAMP,
    error_message TEXT,
    metadata TEXT,
    entity_reference VARCHAR(100),
    entity_type VARCHAR(50)
);

-- Indexes for performance
CREATE INDEX idx_notification_status ON notification_queue(status);
CREATE INDEX idx_notification_type ON notification_queue(notification_type);
CREATE INDEX idx_notification_recipient ON notification_queue(recipient_email);
```

## Error Handling

### Exception Types

1. **ValidationException** - Input validation errors
2. **ResourceNotFoundException** - Notification not found
3. **BusinessLogicException** - Business rule violations
4. **NotificationProcessingException** - Processing errors

### Retry Logic

- **Max Retries**: 3 attempts by default
- **Retry Delay**: Exponential backoff
- **Permanent Failure**: After max retries exceeded
- **Stale Recovery**: Reset stuck processing notifications

## Monitoring and Maintenance

### Health Checks

```bash
# Check queue health
GET /api/notifications/queue/health

# Get processing statistics
GET /api/notifications/queue/processing/statistics

# Get queue statistics
GET /api/notifications/queue/statistics
```

### Maintenance Operations

```bash
# Reset stale notifications
POST /api/notifications/queue/reset-stale?staleMinutes=30

# Clean up old notifications
POST /api/notifications/queue/cleanup?daysToKeep=30
```

## Best Practices

### 1. **Use Queued Methods**
- Prefer `sendNotificationQueued()` over `sendNotification()`
- Use high priority for urgent notifications
- Schedule non-urgent notifications

### 2. **Error Handling**
- Always handle exceptions from notification services
- Implement fallback mechanisms
- Log notification failures

### 3. **Performance**
- Use batch operations when possible
- Monitor queue health regularly
- Adjust thread pool sizes based on load

### 4. **Monitoring**
- Set up alerts for queue health
- Monitor retry rates and failure patterns
- Track processing statistics

## Troubleshooting

### Common Issues

1. **Notifications Not Processing**
   - Check queue health status
   - Verify thread pool configuration
   - Check for stale processing notifications

2. **High Failure Rates**
   - Review error messages
   - Check SMTP/SMS configuration
   - Verify recipient email formats

3. **Performance Issues**
   - Monitor thread pool utilization
   - Check database connection pool
   - Review rate limiting settings

### Debug Commands

```bash
# Check queue health
curl http://localhost:8085/api/notifications/queue/health

# Get statistics
curl http://localhost:8085/api/notifications/queue/statistics

# Reset stale notifications
curl -X POST http://localhost:8085/api/notifications/queue/reset-stale
```

## Conclusion

The notification queue system provides a robust, scalable, and reliable solution for handling notifications while maintaining full backward compatibility with the existing system. The harmonization approach ensures a smooth transition and allows for gradual migration of existing functionality.

The system is now ready for production use with comprehensive monitoring, error handling, and maintenance capabilities.
