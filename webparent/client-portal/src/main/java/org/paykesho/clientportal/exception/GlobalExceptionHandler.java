package org.paykesho.clientportal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Global exception handler for Client Portal
 * Handles all exceptions across the application without changing business logic
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DEFAULT_HOME_URL = "/clients/dashboard";
    private static final String DEFAULT_HOME_BUTTON_TEXT = "Go to Dashboard";

    /**
     * Helper method to add common error attributes
     */
    private void addCommonAttributes(ModelAndView modelAndView, String errorCode, String title, String message) {
        modelAndView.addObject("errorCode", errorCode);
        modelAndView.addObject("title", title);
        modelAndView.addObject("errorMessage", message);
        modelAndView.addObject("homeUrl", DEFAULT_HOME_URL);
        modelAndView.addObject("homeButtonText", DEFAULT_HOME_BUTTON_TEXT);
        modelAndView.addObject("timestamp", LocalDateTime.now());
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        addCommonAttributes(modelAndView, "500", "Internal Server Error", 
            "Something went wrong on our end. Our team has been notified and is working to fix the issue. Please try again later.");
        
        // Add exception details in development mode (you can check for a profile)
        if (logger.isDebugEnabled()) {
            modelAndView.addObject("details", ex.getMessage());
        }

        return modelAndView;
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ModelAndView handleAuthenticationException(Exception ex, HttpServletRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("security/login");
        modelAndView.addObject("errorMessage", "Invalid credentials. Please try again.");
        modelAndView.addObject("errorCode", "401");

        return modelAndView;
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Access denied for user: {} at {}", request.getRemoteUser(), request.getRequestURI());

        ModelAndView modelAndView = new ModelAndView("error/403");
        addCommonAttributes(modelAndView, "403", "Access Denied", 
            "You don't have permission to access this resource. If you believe this is an error, please contact your administrator.");

        return modelAndView;
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("error/400");
        addCommonAttributes(modelAndView, "400", "Bad Request", 
            "The request you sent was invalid or malformed. Please check your input and try again.");
        
        // Add validation field errors if available
        if (ex.getBindingResult() != null && ex.getBindingResult().hasFieldErrors()) {
            StringBuilder details = new StringBuilder();
            ex.getBindingResult().getFieldErrors().forEach(error -> {
                details.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("\n");
            });
            modelAndView.addObject("details", details.toString().trim());
        }

        return modelAndView;
    }

    /**
     * Handle null pointer exceptions
     */
    @ExceptionHandler(NullPointerException.class)
    public ModelAndView handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        logger.error("Null pointer exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        addCommonAttributes(modelAndView, "500", "Internal Server Error", 
            "A system error occurred. Our team has been notified. Please try again later or contact support if the problem persists.");
        
        if (logger.isDebugEnabled()) {
            modelAndView.addObject("details", "NullPointerException: " + ex.getMessage());
        }

        return modelAndView;
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("error/400");
        addCommonAttributes(modelAndView, "400", "Bad Request", 
            "Invalid request parameters. Please check your input and try again.");
        
        if (logger.isDebugEnabled()) {
            modelAndView.addObject("details", ex.getMessage());
        }

        return modelAndView;
    }

    /**
     * Handle REST API exceptions - this will be handled by the generic exception handler above
     * since we can't have two handlers for the same exception type
     */

    /**
     * Handle PaykeshoError exceptions
     */
    @ExceptionHandler(org.commons.utils.PaykeshoError.class)
    public ModelAndView handlePaykeshoError(org.commons.utils.PaykeshoError ex, HttpServletRequest request) {
        logger.error("Paykesho error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        addCommonAttributes(modelAndView, "500", "Application Error", ex.getMessage());
        
        if (logger.isDebugEnabled()) {
            modelAndView.addObject("details", ex.getMessage());
        }

        return modelAndView;
    }
}
