package org.paykesho.backoffice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Custom Error Controller to handle HTTP error pages (404, 500, etc.)
 * This complements the GlobalExceptionHandler for handling errors that don't throw exceptions
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    private static final String DEFAULT_HOME_URL = "/";
    private static final String DEFAULT_HOME_BUTTON_TEXT = "Go to Dashboard";

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            logger.warn("Error {} occurred at {}: {}", statusCode, requestUri, errorMessage);

            ModelAndView modelAndView = new ModelAndView();
            
            // Determine which error page to show based on status code
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                modelAndView.setViewName("error/404");
                addCommonAttributes(modelAndView, "404", "Page Not Found",
                    "Oops! The page you're looking for doesn't exist. It might have been moved, deleted, or the URL might be incorrect.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                modelAndView.setViewName("error/403");
                addCommonAttributes(modelAndView, "403", "Access Denied",
                    "You don't have permission to access this resource. If you believe this is an error, please contact your administrator.");
            } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                modelAndView.setViewName("error/400");
                addCommonAttributes(modelAndView, "400", "Bad Request",
                    "The request you sent was invalid or malformed. Please check your input and try again.");
            } else {
                // Default to 500 error page
                modelAndView.setViewName("error/500");
                addCommonAttributes(modelAndView, String.valueOf(statusCode), "Server Error",
                    "Something went wrong on our end. Our team has been notified and is working to fix the issue. Please try again later.");
            }

            // Add exception details if available (only in debug mode)
            if (exception != null && logger.isDebugEnabled()) {
                modelAndView.addObject("details", exception.toString());
            }

            return modelAndView;
        }

        // Fallback if status is not available
        ModelAndView modelAndView = new ModelAndView("error/500");
        addCommonAttributes(modelAndView, "500", "Internal Server Error",
            "An unexpected error occurred. Please try again later.");
        return modelAndView;
    }

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
}

