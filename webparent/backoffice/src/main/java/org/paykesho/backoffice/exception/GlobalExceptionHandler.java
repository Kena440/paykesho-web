package org.paykesho.backoffice.exception;

import lombok.extern.slf4j.Slf4j;
import org.commons.utils.PaykeshoError;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced Global Exception Handler for Back Office
 * Handles all exceptions across the application with comprehensive error responses
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle PaykeshoError (Business Logic Errors) - Handle first for priority
     */
    @ExceptionHandler(PaykeshoError.class)
    public ModelAndView handlePaykeshoError(PaykeshoError ex, HttpServletRequest request) {
        log.error("Business logic error: {} at {}", ex.getMessage(), request.getRequestURI());

        ModelAndView modelAndView = new ModelAndView("error/business-error");
        modelAndView.addObject("errorMessage", ex.getMessage());
        modelAndView.addObject("errorCode", "BUSINESS_ERROR");
        modelAndView.addObject("timestamp", LocalDateTime.now());
        modelAndView.addObject("path", request.getRequestURI());

        return modelAndView;
    }

    /**
     * Handle AJAX requests with JSON responses
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleAjaxException(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {

        // Check if it's an AJAX request
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");

        if ("XMLHttpRequest".equals(requestedWith) ||
                (accept != null && accept.contains("application/json"))) {

            log.error("AJAX error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

            Map<String, Object> errorResponse = createErrorResponse(
                    "AJAX_ERROR",
                    "Request failed. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    request.getRequestURI()
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }

        // For non-AJAX requests, fall through to other handlers
        return handleGenericException(ex, request);
    }

    /**
     * Handle general exceptions (non-AJAX)
     */
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        modelAndView.addObject("errorMessage", "An unexpected error occurred. Please try again later.");
        modelAndView.addObject("errorCode", "INTERNAL_ERROR");
        modelAndView.addObject("timestamp", LocalDateTime.now());

        // Return JSON for programmatic handling
        Map<String, Object> errorResponse = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(errorResponse);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ModelAndView handleAuthenticationException(Exception ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("security/login");
        modelAndView.addObject("errorMessage", "Invalid credentials. Please check your email and password.");
        modelAndView.addObject("errorCode", "AUTH_FAILED");

        return modelAndView;
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for user {} at {}: {}", request.getRemoteUser(), request.getRequestURI(), ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("error/403");
        modelAndView.addObject("errorMessage", "You don't have permission to access this resource.");
        modelAndView.addObject("errorCode", "ACCESS_DENIED");
        modelAndView.addObject("timestamp", LocalDateTime.now());

        return modelAndView;
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Map<String, Object>> handleValidationException(Exception ex, HttpServletRequest request) {
        log.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "VALIDATION_ERROR",
                "Invalid input data provided",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI()
        );

        // Add field-specific errors
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validationEx = (MethodArgumentNotValidException) ex;
            Map<String, String> fieldErrors = validationEx.getBindingResult().getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            errorResponse.put("fieldErrors", fieldErrors);
        }

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions
     * Note: javax.validation is not available in current Spring Boot version
     */
    // @ExceptionHandler(ConstraintViolationException.class)
    // public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
    //         ConstraintViolationException ex, HttpServletRequest request) {
    //     log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());
    //     
    //     Map<String, Object> errorResponse = createErrorResponse(
    //         "CONSTRAINT_VIOLATION", 
    //         "Data validation failed",
    //         HttpStatus.BAD_REQUEST,
    //         request.getRequestURI()
    //     );
    //     
    //     Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
    //     Map<String, String> constraintErrors = violations.stream()
    //         .collect(Collectors.toMap(
    //             violation -> violation.getPropertyPath().toString(),
    //             ConstraintViolation::getMessage
    //         ));
    //     errorResponse.put("constraintErrors", constraintErrors);
    //     
    //     return ResponseEntity.badRequest().body(errorResponse);
    // }

    /**
     * Handle database access exceptions
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<Map<String, Object>> handleDatabaseException(Exception ex, HttpServletRequest request) {
        log.error("Database error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorResponse = createErrorResponse(
                "DATABASE_ERROR",
                "A database error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(errorResponse);
    }

    /**
     * Handle file upload size exceeded exceptions
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload size exceeded at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "FILE_SIZE_EXCEEDED",
                "File size exceeds maximum allowed limit",
                HttpStatus.PAYLOAD_TOO_LARGE,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handle null pointer exceptions
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(
            NullPointerException ex, HttpServletRequest request) {
        log.error("Null pointer exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorResponse = createErrorResponse(
                "NULL_POINTER",
                "A system error occurred due to missing data. Please contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "ILLEGAL_ARGUMENT",
                "Invalid request parameters provided",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, HttpStatus status, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path);

        return errorResponse;
    }
}
