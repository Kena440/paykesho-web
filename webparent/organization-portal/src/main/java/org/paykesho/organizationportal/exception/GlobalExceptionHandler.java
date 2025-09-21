package org.paykesho.organizationportal.exception;

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

/**
 * Global exception handler for Organization Portal
 * Handles all exceptions across the application without changing business logic
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        modelAndView.addObject("errorMessage", "An unexpected error occurred. Please try again later.");
        modelAndView.addObject("errorCode", "500");
        modelAndView.addObject("timestamp", System.currentTimeMillis());

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
        logger.warn("Access denied for user: {}", request.getRemoteUser());

        ModelAndView modelAndView = new ModelAndView("error/403");
        modelAndView.addObject("errorMessage", "You don't have permission to access this resource.");
        modelAndView.addObject("errorCode", "403");

        return modelAndView;
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("error/400");
        modelAndView.addObject("errorMessage", "Invalid input data. Please check your form and try again.");
        modelAndView.addObject("errorCode", "400");

        return modelAndView;
    }

    /**
     * Handle null pointer exceptions
     */
    @ExceptionHandler(NullPointerException.class)
    public ModelAndView handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        logger.error("Null pointer exception: {}", ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        modelAndView.addObject("errorMessage", "A system error occurred. Please contact support.");
        modelAndView.addObject("errorCode", "500");

        return modelAndView;
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());

        ModelAndView modelAndView = new ModelAndView("error/400");
        modelAndView.addObject("errorMessage", "Invalid request parameters. Please check your input.");
        modelAndView.addObject("errorCode", "400");

        return modelAndView;
    }

    /**
     * Handle REST API exceptions - handled by generic exception handler above
     */

    /**
     * Handle PaykeshoError exceptions
     */
    @ExceptionHandler(org.commons.utils.PaykeshoError.class)
    public ModelAndView handlePaykeshoError(org.commons.utils.PaykeshoError ex, HttpServletRequest request) {
        logger.error("Paykesho error: {}", ex.getMessage(), ex);

        ModelAndView modelAndView = new ModelAndView("error/500");
        modelAndView.addObject("errorMessage", ex.getMessage());
        modelAndView.addObject("errorCode", "500");

        return modelAndView;
    }
}
