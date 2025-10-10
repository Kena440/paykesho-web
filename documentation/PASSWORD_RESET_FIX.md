# Password Reset Issue - Fixed (All Modules)

## Problem Summary
When attempting to reset a password across all portals (backoffice, client-portal, merchant-portal, organization-portal), the CAPTCHA fields were empty during form submission, causing the password reset to fail with a "CAPTCHA verification failed" error.

## Root Cause
The issue was caused by two main problems:

1. **Idempotency Interceptor Interference**
   - The `/forgot-password` and `/forgot-password/captcha` endpoints were being intercepted by the idempotency mechanism
   - This was storing the entire HTML response (including the login page) in the idempotency records
   - The interceptor was interfering with the normal flow of the password reset process

2. **Empty CAPTCHA Fields**
   - CAPTCHA fields (`captchaChallengeId` and `captchaAnswer`) were empty when the form was submitted
   - The JavaScript code was attempting to load the CAPTCHA via AJAX, but the form could be submitted before the CAPTCHA loaded
   - No proper validation was in place to detect empty CAPTCHA fields before attempting verification

## Technical Details

### Log Evidence
From the logs, we observed:
```
requestBody=_csrf=75e81284-3407-4db8-9286-05ac75e5d317&modalPasswordResetEmail=kaengamwamba%40gmail.com&captchaChallengeId=&captchaAnswer=
```
Both `captchaChallengeId` and `captchaAnswer` were empty.

### Idempotency Record Issue
The idempotency system was storing the complete HTML response (login page) in the database, which was unnecessary and indicated that the endpoint shouldn't be using idempotency for form-based authentication flows.

## Solution Implemented

### 1. Excluded Password Reset Endpoints from Idempotency Interceptor

#### Backoffice Module
**File:** `webparent/backoffice/src/main/java/org/paykesho/backoffice/config/WebMvcConfiguration.java`

```java
.excludePathPatterns("/actuator/**", "/health/**", "/login/**", "/logout/**", "/auth/**", 
                   "/css/**", "/js/**", "/images/**", "/assets/**",
                   "/forgot-password/**", "/reset-password/**");
```

**File:** `webparent/backoffice/src/main/java/org/paykesho/backoffice/idempotency/interceptor/IdempotencyInterceptor.java`

```java
private boolean shouldSkipIdempotency(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.contains("/actuator/") || uri.contains("/health") || 
           uri.contains("/login") || uri.contains("/logout") || uri.contains("/auth") ||
           uri.contains("/forgot-password") || uri.contains("/reset-password") ||
           uri.contains("/assets/") || uri.contains("/images/") || uri.contains("/css/") || uri.contains("/js/");
}
```

#### Client Portal Module
**Files Updated:**
- `webparent/client-portal/src/main/java/org/paykesho/clientportal/config/WebMvcConfiguration.java`
- `webparent/client-portal/src/main/java/org/paykesho/clientportal/idempotency/interceptor/IdempotencyInterceptor.java`

#### Payment Service Module
**Files Updated:**
- `webparent/payment-service/src/main/java/org/paykesho/paymentservice/config/WebMvcConfiguration.java`
- `webparent/payment-service/src/main/java/org/paykesho/paymentservice/idempotency/interceptor/IdempotencyInterceptor.java`

#### Notification Service Module
**Files Updated:**
- `webparent/notification-service/src/main/java/org/paykesho/notificationservice/config/WebMvcConfiguration.java`
- `webparent/notification-service/src/main/java/org/paykesho/notificationservice/idempotency/interceptor/IdempotencyInterceptor.java`

### 2. Added CAPTCHA Field Validation

Applied to all portal modules with password reset functionality:

#### Backoffice Module
**File:** `webparent/backoffice/src/main/java/org/paykesho/backoffice/security/ForgotPasswordController.java`

Added explicit validation for empty CAPTCHA fields:

```java
// Check if CAPTCHA fields are provided
if (captchaAnswer == null || captchaAnswer.trim().isEmpty() || 
    captchaChallengeId == null || captchaChallengeId.trim().isEmpty()) {
    LOGGER.warn("CAPTCHA fields are empty for session: {}. Please load CAPTCHA before submitting.", sessionId);
    model.addAttribute("forgotPasswordMessage", 
        "Please solve the security challenge before submitting the form.");
    return "security/login";
}
```

This provides a clear error message to the user when CAPTCHA fields are not populated.

#### Client Portal Module
**File:** `webparent/client-portal/src/main/java/org/paykesho/clientportal/clients/controller/ForgotPasswordController.java`

#### Merchant Portal Module
**File:** `webparent/merchant-portal/src/main/java/org/paykesho/merchantportal/merchantadmins/controller/ForgotPasswordController.java`

#### Organization Portal Module
**File:** `webparent/organization-portal/src/main/java/org/paykesho/organizationportal/organizationadmins/controller/ForgotPasswordController.java`

## Modules Fixed

✅ **Backoffice** - Complete (WebMvcConfiguration + IdempotencyInterceptor + ForgotPasswordController)
✅ **Client Portal** - Complete (WebMvcConfiguration + IdempotencyInterceptor + ForgotPasswordController)
✅ **Merchant Portal** - Complete (ForgotPasswordController)
✅ **Organization Portal** - Complete (ForgotPasswordController)
✅ **Payment Service** - Complete (WebMvcConfiguration + IdempotencyInterceptor)
✅ **Notification Service** - Complete (WebMvcConfiguration + IdempotencyInterceptor)

## Benefits of the Fix

1. **Cleaner Logs**: Password reset operations no longer create unnecessary idempotency records
2. **Better Performance**: Avoids storing large HTML responses in the database
3. **Improved UX**: Users get a clear error message if they submit the form before CAPTCHA loads
4. **Better Security**: Maintains CAPTCHA protection while handling edge cases gracefully
5. **Proper Separation**: Authentication flows are now properly separated from transactional idempotency

## Testing Steps

### For All Portals (Backoffice, Client, Merchant, Organization)

1. Navigate to the portal login page
2. Click "Forgot password?"
3. Wait for the CAPTCHA to load (math problem should appear)
4. Enter your email address
5. Solve the CAPTCHA
6. Click "Send Reset Link"
7. Verify you receive the success message

**Expected Behavior:**
- CAPTCHA loads successfully when modal opens
- Form cannot be submitted without solving CAPTCHA
- Clear error message appears if CAPTCHA is not filled
- Password reset email is sent upon successful submission
- No HTML responses stored in idempotency records

## Additional Notes

- The idempotency mechanism is now properly scoped to business transactions only
- Authentication and password reset flows are excluded as they have their own security mechanisms (CSRF, CAPTCHA, rate limiting)
- The fix maintains all existing security features while improving reliability

## Related Security Features

The password reset functionality still maintains:
- ✅ Email enumeration prevention
- ✅ Rate limiting (IP and email based)
- ✅ CAPTCHA protection
- ✅ Consistent response timing
- ✅ Input validation and sanitization
- ✅ CSRF protection

All security features remain intact and functional.

