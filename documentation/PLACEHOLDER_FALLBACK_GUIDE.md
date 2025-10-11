# Placeholder & Fallback Implementation Guide

## Overview

The file service now includes robust fallback mechanisms to handle missing or inaccessible files gracefully. Instead of showing broken images or errors, the system displays appropriate placeholders.

## Features

### 1. Automatic Placeholder Serving

When a file is missing or inaccessible, the file-service automatically returns a placeholder image instead of a 404 error.

### 2. Type-Specific Placeholders

Different placeholder images for different file types:
- **Logo**: Shield icon with "Logo Missing" text
- **Photo**: Avatar icon with "Photo Missing" text
- **Payslip**: Document icon with "Document Missing" text
- **Generic**: File icon with "File Missing" text

### 3. Fallback Methods in Services

Services now have fallback methods that automatically return placeholder URLs when file access fails.

## Implementation

### File Service Controller

#### PlaceholderController
Serves placeholder images for missing files:

```java
GET /placeholders/logo      // Returns logo placeholder
GET /placeholders/photo     // Returns photo placeholder
GET /placeholders/payslip   // Returns payslip placeholder
GET /placeholders/{type}    // Returns placeholder for any FileType
```

**Example Response:**
- SVG image with appropriate icon and text
- Cached for 24 hours (reduces server load)
- Graceful fallback if custom placeholders not available

### Enhanced FileUploadService

#### New Methods

**1. getFileAccessDetailsWithFallback()**
```java
// Automatically returns placeholder on failure
FileAccessResponse response = fileUploadService.getFileAccessDetailsWithFallback(
    fileId, 
    FileType.logo
);

// response.fileUrl will be either:
// - Actual file URL if found
// - Placeholder URL if missing
```

**2. getPlaceholderUrl()**
```java
// Get placeholder URL for a specific file type
String placeholderUrl = fileUploadService.getPlaceholderUrl(FileType.photo);
// Returns: "http://file-service:8086/placeholders/photo"
```

### Usage Examples

#### Example 1: Service Layer (Backoffice)

**Before (No Fallback)**:
```java
// UserService.java
FileAccessResponse fileAccess = fileUploadService.getFileAccessDetails(user.getRemoteFileId());
if (!fileAccess.isStatus()) {
    // Handle error - maybe show nothing or error message
    user.setPhotoUrl(null);
}
```

**After (With Fallback)**:
```java
// UserService.java
FileAccessResponse fileAccess = fileUploadService.getFileAccessDetailsWithFallback(
    user.getRemoteFileId(),
    FileType.photo
);
// Always returns a valid URL (actual file or placeholder)
user.setPhotoUrl(fileAccess.getFileUrl());
```

#### Example 2: Controller Layer

**Before**:
```java
// OrganizationController.java
if (organization.getRemoteFileId() != null) {
    FileAccessResponse fileAccess = fileUploadService.getFileAccessDetails(
        organization.getRemoteFileId()
    );
    model.addAttribute("logoUrl", fileAccess.getFileUrl());
} else {
    model.addAttribute("logoUrl", "/images/default-logo.png");
}
```

**After**:
```java
// OrganizationController.java
String logoUrl = organization.getRemoteFileId() != null
    ? fileUploadService.getFileAccessDetailsWithFallback(
        organization.getRemoteFileId(), 
        FileType.logo
      ).getFileUrl()
    : fileUploadService.getPlaceholderUrl(FileType.logo);
    
model.addAttribute("logoUrl", logoUrl);
```

#### Example 3: Thymeleaf Template

**HTML Template Usage**:
```html
<!-- User photo with fallback -->
<img th:src="${user.photoUrl}" 
     th:onerror="'this.src=\'' + ${@fileUploadService.getPlaceholderUrl(T(org.commons.enums.FileType).photo)} + '\';'"
     alt="User Photo"
     class="avatar">

<!-- Organization logo with fallback -->
<img th:src="${organization.logoUrl ?: '/placeholders/logo'}" 
     alt="Organization Logo"
     class="logo">

<!-- Merchant logo -->
<div class="merchant-logo">
    <img th:src="${merchant.logoUrl}" 
         th:onerror="'this.src=\'/placeholders/logo\';'"
         alt="Merchant Logo">
</div>
```

### JavaScript Fallback (Frontend)

For additional resilience, add JavaScript fallback:

```javascript
// Global image error handler
document.addEventListener('error', function(e) {
    if (e.target.tagName === 'IMG') {
        const img = e.target;
        
        // Prevent infinite loop
        if (img.hasAttribute('data-fallback-failed')) {
            return;
        }
        
        // Determine placeholder based on image class
        let placeholder = '/placeholders/file';
        if (img.classList.contains('logo')) {
            placeholder = '/placeholders/logo';
        } else if (img.classList.contains('avatar') || img.classList.contains('photo')) {
            placeholder = '/placeholders/photo';
        } else if (img.classList.contains('document')) {
            placeholder = '/placeholders/payslip';
        }
        
        img.setAttribute('data-fallback-failed', 'true');
        img.src = placeholder;
    }
}, true);
```

### Utility Class

Use the `FileAccessUtil` helper class:

```java
import org.commons.utils.FileAccessUtil;

// Get file URL with automatic fallback
String safeUrl = FileAccessUtil.getFileUrlOrPlaceholder(
    fileUrl, 
    FileType.logo, 
    "http://file-service:8086"
);

// Check if URL is a placeholder
boolean isPlaceholder = FileAccessUtil.isPlaceholder(fileUrl);

// Get safe file URL with validation
String url = FileAccessUtil.getSafeFileUrl(
    maybeInvalidUrl, 
    FileType.photo, 
    baseUrl
);
```

## Placeholder Endpoints

### Available Endpoints

| Endpoint | Description | Returns |
|----------|-------------|---------|
| `/placeholders/logo` | Logo placeholder | Shield icon + "Logo Missing" |
| `/placeholders/photo` | Photo/avatar placeholder | Avatar icon + "Photo Missing" |
| `/placeholders/payslip` | Document placeholder | Document icon + "Document Missing" |
| `/placeholders/{type}` | Any FileType placeholder | Type-specific placeholder |
| `/placeholders/file` | Generic file placeholder | File icon + "File Missing" |

### Placeholder Features

- **SVG Format**: Scalable and lightweight
- **Caching**: 24-hour cache for performance
- **Icons**: Material Design-inspired icons
- **Colors**: Type-specific colors
- **Fallback**: Generated SVG if image files missing

## Migration Guide

### Step 1: Update Service Calls

Replace direct `getFileAccessDetails()` calls with fallback version:

```java
// Old
FileAccessResponse response = fileUploadService.getFileAccessDetails(fileId);

// New
FileAccessResponse response = fileUploadService.getFileAccessDetailsWithFallback(
    fileId, 
    FileType.logo  // or appropriate type
);
```

### Step 2: Handle Null Remote File IDs

```java
// Before
if (entity.getRemoteFileId() != null) {
    FileAccessResponse response = fileUploadService.getFileAccessDetails(
        entity.getRemoteFileId()
    );
    entity.setImageUrl(response.getFileUrl());
}

// After
String imageUrl = entity.getRemoteFileId() != null
    ? fileUploadService.getFileAccessDetailsWithFallback(
        entity.getRemoteFileId(), 
        FileType.photo
      ).getFileUrl()
    : fileUploadService.getPlaceholderUrl(FileType.photo);
    
entity.setImageUrl(imageUrl);
```

### Step 3: Update Templates

Add `onerror` handlers to images:

```html
<!-- Add onerror attribute -->
<img th:src="${imageUrl}" 
     onerror="this.src='/placeholders/logo';" 
     alt="Logo">
```

## Benefits

### 1. **Better User Experience**
- No broken images
- Clear indication when files are missing
- Professional appearance

### 2. **Easier Debugging**
- Logs indicate when placeholders are served
- Can identify missing files easily
- No 404 errors in browser console

### 3. **Resilience**
- Service continues working even if files are deleted
- Handles network issues gracefully
- No application crashes

### 4. **Performance**
- Placeholders are cached
- Lightweight SVG format
- Reduces unnecessary error logging

## Testing

### Test Placeholder Serving

```bash
# Test missing file (should return placeholder)
curl http://localhost:8086/uploads/logo/nonexistent-file.png

# Test placeholder endpoints directly
curl http://localhost:8086/placeholders/logo
curl http://localhost:8086/placeholders/photo
curl http://localhost:8086/placeholders/payslip
```

### Test Fallback Method

```java
@Test
public void testFallbackReturnsPlaceholder() {
    // Test with invalid file ID
    FileAccessResponse response = fileUploadService
        .getFileAccessDetailsWithFallback(999999, FileType.logo);
    
    assertTrue(response.isStatus());
    assertTrue(response.getFileUrl().contains("/placeholders/"));
}
```

## Customization

### Custom Placeholder Images

To use custom placeholder images instead of generated SVGs:

1. Create placeholder images:
   ```
   src/main/resources/
   └── placeholders/
       ├── default-logo.png
       ├── default-avatar.png
       ├── default-document.png
       └── default-file.png
   ```

2. Place images in resources folder

3. File-service will automatically use them if available

### Custom Colors

Edit `PlaceholderController.java` to customize colors:

```java
case "logo":
    color = "#4F46E5";  // Change to your brand color
    break;
```

## Best Practices

### 1. Always Use Fallback Methods
```java
// ✅ Good
response = fileUploadService.getFileAccessDetailsWithFallback(id, type);

// ❌ Avoid
response = fileUploadService.getFileAccessDetails(id);
if (!response.isStatus()) {
    // Handle error manually
}
```

### 2. Specify Correct File Type
```java
// ✅ Good - provides correct placeholder
getFileAccessDetailsWithFallback(id, FileType.logo);

// ⚠️ Works but generic placeholder
getFileAccessDetailsWithFallback(id, null);
```

### 3. Add Frontend Fallback
```html
<!-- ✅ Good - double protection -->
<img th:src="${url}" onerror="this.src='/placeholders/logo';">

<!-- ⚠️ Less resilient -->
<img th:src="${url}">
```

### 4. Log Placeholder Usage
The service automatically logs when placeholders are served:
```
INFO: Returning placeholder for file ID 123 of type logo
```

Monitor these logs to identify missing files that need attention.

## Troubleshooting

### Issue: Placeholder Not Showing
**Solution**: Check file-service is running and accessible

### Issue: Custom Placeholder Not Loading
**Solution**: Verify file path and resource location

### Issue: Infinite Loop with onerror
**Solution**: Use `data-fallback-failed` attribute as shown in JavaScript example

## Summary

The placeholder/fallback system provides:
- ✅ Automatic fallback for missing files
- ✅ Type-specific placeholder images
- ✅ Graceful error handling
- ✅ Better user experience
- ✅ Easy migration path
- ✅ Customizable placeholders

All file access is now resilient and user-friendly!

---

*Last Updated: October 2024*

