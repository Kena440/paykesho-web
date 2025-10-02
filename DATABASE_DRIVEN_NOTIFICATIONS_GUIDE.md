# Database-Driven Notifications Implementation Guide

## 🎯 **Overview**

This guide explains the implementation of database-driven notifications that eliminate inter-modular HTTP communication by using the shared database as the communication layer.

## 🏗️ **Architecture**

### **Core Concept:**
- **Instead of HTTP calls** → **Direct database writes**
- **Instead of service dependencies** → **Database polling**
- **Instead of circuit breakers** → **Database transactions**

### **Benefits:**
- ✅ **No network dependencies** - No HTTP calls between modules
- ✅ **No circuit breakers** - No service availability issues
- ✅ **No retry logic** - Database handles persistence
- ✅ **Better reliability** - Database transactions ensure consistency
- ✅ **Simpler debugging** - All data in one place

## 📊 **Database Schema**

### **NotificationRequest Table:**
```sql
CREATE TABLE notification_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL,
    scheduled_for TIMESTAMP,
    last_processed_at TIMESTAMP,
    notification_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 3,
    recipient_email VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20),
    recipient_name VARCHAR(128),
    subject VARCHAR(255),
    content TEXT,
    template_name VARCHAR(100),
    template_variables TEXT,
    source_module VARCHAR(50) NOT NULL,
    entity_reference VARCHAR(100),
    entity_type VARCHAR(50),
    metadata TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    last_error TEXT,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    INDEX idx_notification_request_status (status),
    INDEX idx_notification_request_type (notification_type),
    INDEX idx_notification_request_recipient (recipient_email),
    INDEX idx_notification_request_module (source_module),
    INDEX idx_notification_request_priority (priority)
);
```

## 🔧 **Implementation Details**

### **1. Shared Entities (commons module)**

#### **NotificationRequest Entity:**
```java
@Entity
@Table(name = "notification_requests")
public class NotificationRequest extends IdBasedEntity {
    // Fields for notification details, status, scheduling, etc.
    // Helper methods: isReadyForProcessing(), markAsProcessing(), etc.
}
```

#### **DatabaseNotificationService Interface:**
```java
public interface DatabaseNotificationService {
    NotificationRequest createNotificationRequest(NotificationType type, String recipient, 
                                                String subject, String content, String sourceModule);
    NotificationRequest createHighPriorityNotificationRequest(...);
    NotificationRequest createScheduledNotificationRequest(...);
    // ... other methods
}
```

### **2. Implementation (notification-service module)**

#### **DatabaseNotificationServiceImpl:**
- Implements the interface
- Uses `NotificationRequestRepository` for database operations
- Handles all CRUD operations for notification requests

#### **NotificationRequestRepository:**
- JPA repository with custom queries
- Methods for finding pending, processing, failed notifications
- Statistics and cleanup queries

### **3. Enhanced Existing Services**

#### **Backoffice NotificationClient:**
```java
@Service
public class NotificationClient {
    @Autowired
    private DatabaseNotificationService databaseNotificationService;
    
    public boolean sendNotification(String recipient, String message, String notificationType) {
        // Try database-driven approach first (recommended)
        try {
            NotificationRequest request = databaseNotificationService.createNotificationRequest(
                NotificationType.valueOf(notificationType.toUpperCase()), 
                recipient, "Notification", message, "backoffice"
            );
            return true;
        } catch (Exception e) {
            // Fallback to HTTP approach
            return sendNotificationViaHttp(recipient, message, notificationType);
        }
    }
}
```

#### **NotificationProcessorService:**
```java
@Service
public class NotificationProcessorService {
    @Autowired
    private DatabaseNotificationService databaseNotificationService;
    
    @Scheduled(fixedRate = 15000)
    public void processDatabaseNotificationRequests() {
        List<NotificationRequest> pendingRequests = 
            databaseNotificationService.getPendingNotificationsReadyForProcessing();
        
        for (NotificationRequest request : pendingRequests) {
            processDatabaseNotificationRequest(request);
        }
    }
}
```

## 🚀 **Usage Examples**

### **Basic Notification:**
```java
// In any module
@Autowired
private DatabaseNotificationService databaseNotificationService;

// Create notification request
NotificationRequest request = databaseNotificationService.createNotificationRequest(
    NotificationType.email, 
    "user@example.com", 
    "Welcome", 
    "Welcome to our platform!", 
    "client-portal"
);
```

### **High Priority Notification:**
```java
NotificationRequest request = databaseNotificationService.createHighPriorityNotificationRequest(
    NotificationType.sms, 
    "+1234567890", 
    "Urgent Alert", 
    "Your account has been compromised!", 
    "security-module"
);
```

### **Scheduled Notification:**
```java
Date scheduledFor = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24 hours from now
NotificationRequest request = databaseNotificationService.createScheduledNotificationRequest(
    NotificationType.email, 
    "user@example.com", 
    "Reminder", 
    "Don't forget to complete your profile!", 
    "user-module", 
    scheduledFor
);
```

### **Password Reset Notification:**
```java
NotificationRequest request = databaseNotificationService.createPasswordResetNotificationRequest(
    "user@example.com", 
    "John Doe", 
    "https://app.paykesho.com/reset?token=abc123", 
    "backoffice"
);
```

## 📈 **Processing Flow**

### **1. Creation Phase:**
```
Module → DatabaseNotificationService → NotificationRequest (PENDING) → Database
```

### **2. Processing Phase:**
```
NotificationProcessorService → Polls Database → Finds PENDING requests → Processes → Updates Status
```

### **3. Status Flow:**
```
PENDING → PROCESSING → SENT/FAILED → (Retry if FAILED) → SENT/FAILED_PERMANENTLY
```

## 🔍 **Monitoring & Statistics**

### **Get Statistics:**
```java
Map<String, Object> stats = databaseNotificationService.getNotificationStatistics();
// Returns: {total: 1000, pending: 50, processing: 5, sent: 900, failed: 45}
```

### **Get Notifications by Status:**
```java
List<NotificationRequest> pending = databaseNotificationService.getNotificationsByStatus(NotificationStatus.PENDING);
List<NotificationRequest> failed = databaseNotificationService.getNotificationsByStatus(NotificationStatus.FAILED);
```

### **Get Notifications by Module:**
```java
List<NotificationRequest> backofficeNotifications = 
    databaseNotificationService.getNotificationsBySourceModule("backoffice");
```

## 🛠️ **Configuration**

### **Processing Intervals:**
- **Database requests**: Every 15 seconds
- **Queue notifications**: Every 30 seconds
- **Cleanup**: Every hour
- **Stale reset**: Every 30 minutes

### **Rate Limiting:**
- **Delay between requests**: 100ms
- **Batch size**: Configurable
- **Max concurrent**: Configurable

## 🔄 **Migration Strategy**

### **Phase 1: Add Database Support**
- ✅ Create `NotificationRequest` entity
- ✅ Create `DatabaseNotificationService` interface
- ✅ Implement service in notification-service module

### **Phase 2: Enhance Existing Clients**
- ✅ Update `NotificationClient` in backoffice
- ✅ Add database-driven methods to existing services
- ✅ Maintain HTTP fallback for backward compatibility

### **Phase 3: Add Processing**
- ✅ Enhance `NotificationProcessorService`
- ✅ Add scheduled processing for database requests
- ✅ Add monitoring and statistics

### **Phase 4: Gradual Migration**
- 🔄 Update other modules (client-portal, merchant-portal, etc.)
- 🔄 Remove HTTP dependencies gradually
- 🔄 Add comprehensive monitoring

## 📋 **Module Integration**

### **Backoffice Module:**
```java
// Enhanced NotificationClient
public boolean sendNotification(String recipient, String message, String notificationType) {
    // Database-first approach with HTTP fallback
}
```

### **Client Portal Module:**
```java
// Similar enhancement to existing notification services
@Autowired
private DatabaseNotificationService databaseNotificationService;
```

### **Merchant Portal Module:**
```java
// Enhance existing notification handling
// Add database-driven approach
```

## 🎯 **Key Benefits Achieved**

1. **✅ Eliminated HTTP Dependencies** - No more network calls between modules
2. **✅ Removed Circuit Breakers** - No more service availability issues  
3. **✅ Simplified Error Handling** - Database transactions handle consistency
4. **✅ Better Performance** - Direct database writes are faster than HTTP
5. **✅ Enhanced Reliability** - Database persistence ensures no lost notifications
6. **✅ Easier Debugging** - All notification data in one place
7. **✅ Better Monitoring** - Rich statistics and status tracking

## 🚨 **Important Notes**

- **Backward Compatibility**: HTTP fallback maintained for existing functionality
- **Gradual Migration**: Can migrate modules one by one
- **Database Performance**: Proper indexing for efficient queries
- **Monitoring**: Comprehensive logging and statistics
- **Error Handling**: Robust error handling with retry mechanisms

This database-driven approach provides a solid foundation for reliable, scalable notification processing while eliminating the complexity of inter-modular HTTP communication.
