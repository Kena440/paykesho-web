/**
 * Paykesho UI Error Handling Library
 * Provides comprehensive error handling for frontend components
 */

// Global error tracking
window.PaykeshoErrorHandler = {
    errors: [],
    maxErrors: 50,

    // Log error with context
    logError: function (error, context) {
        const errorEntry = {
            timestamp: new Date().toISOString(),
            error: error.toString(),
            context: context || 'unknown',
            stack: error.stack || 'no stack trace',
            url: window.location.href,
            userAgent: navigator.userAgent
        };

        this.errors.push(errorEntry);

        // Keep only recent errors
        if (this.errors.length > this.maxErrors) {
            this.errors = this.errors.slice(-this.maxErrors);
        }

        console.error('Paykesho Error:', errorEntry);

        // Send to server for monitoring (optional)
        this.reportError(errorEntry);
    },

    // Report error to server
    reportError: function (errorEntry) {
        try {
            fetch('/api/errors/report', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(errorEntry)
            }).catch(e => {
                console.warn('Failed to report error to server:', e);
            });
        } catch (e) {
            // Silent fail - don't break the app for error reporting
        }
    }
};

// Global error handler
window.addEventListener('error', function (event) {
    const error = event.error;

    // Don't show alerts for common DOM element not found errors
    if (error && error.message &&
        (error.message.includes('querySelector') ||
            error.message.includes('Cannot read properties of null') ||
            error.message.includes('reading \'querySelector\''))) {
        PaykeshoErrorHandler.logError(error, 'dom-element');
        console.warn('DOM element not found - this is usually harmless:', error.message);
        return;
    }

    PaykeshoErrorHandler.logError(error, 'global');
    showErrorAlert('An unexpected error occurred. Please refresh the page if the problem persists.');
});

// Unhandled promise rejection handler
window.addEventListener('unhandledrejection', function (event) {
    PaykeshoErrorHandler.logError(new Error(event.reason), 'promise');
    showErrorAlert('A network or processing error occurred. Please try again.');
});

// Safe jQuery document ready
function safeDocumentReady(callback) {
    try {
        if (typeof $ !== 'undefined') {
            $(document).ready(function () {
                try {
                    callback();
                } catch (error) {
                    PaykeshoErrorHandler.logError(error, 'document-ready');
                    showErrorAlert('Failed to initialize page components.');
                }
            });
        } else {
            console.error('jQuery is not loaded');
            showErrorAlert('Required libraries are not loaded. Please refresh the page.');
        }
    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'safe-document-ready');
    }
}

// Safe AJAX wrapper
function safeAjaxCall(options) {
    const defaults = {
        timeout: 30000, // 30 seconds
        retries: 3,
        retryDelay: 1000
    };

    const config = Object.assign({}, defaults, options);

    function attemptRequest(attempt) {
        return $.ajax({
            url: config.url,
            method: config.method || 'GET',
            data: config.data,
            dataType: config.dataType || 'json',
            timeout: config.timeout,
            headers: config.headers || {}
        }).fail(function (xhr, status, error) {
            const errorMsg = `AJAX Error: ${status} - ${error}`;
            PaykeshoErrorHandler.logError(new Error(errorMsg), 'ajax');

            if (attempt < config.retries) {
                console.warn(`AJAX request failed, retrying in ${config.retryDelay}ms (attempt ${attempt + 1}/${config.retries})`);
                setTimeout(() => attemptRequest(attempt + 1), config.retryDelay * attempt);
            } else {
                if (config.error) {
                    config.error(xhr, status, error);
                } else {
                    showErrorAlert('Network request failed. Please check your connection and try again.');
                }
            }
        });
    }

    return attemptRequest(1);
}

/**
 * DataTables Utility Functions
 * 
 * USAGE GUIDELINES:
 * - Use DataTables for server-side rendered tables (static HTML from Thymeleaf)
 * - DO NOT use DataTables for tables populated dynamically via AJAX
 * - If you must use DataTables with AJAX-loaded data, destroy it before reloading
 */

/**
 * Safely destroy a DataTable instance if it exists
 * Use this before clearing/reloading table data via AJAX
 * 
 * @param {string} selector - jQuery selector for the table element
 * @returns {boolean} - true if DataTable was destroyed, false otherwise
 */
function destroyDataTableIfExists(selector) {
    try {
        if (typeof $.fn.DataTable === 'undefined') {
            return false;
        }

        const element = $(selector);
        if (element.length === 0) {
            return false;
        }

        if ($.fn.DataTable.isDataTable(selector)) {
            element.DataTable().destroy();
            console.log('DataTable destroyed for:', selector);
            return true;
        }

        return false;
    } catch (error) {
        console.warn('Error destroying DataTable (may not be initialized):', error);
        PaykeshoErrorHandler.logError(error, 'datatable-destroy');
        return false;
    }
}

/**
 * Safe DataTable initialization
 * Use this for server-side rendered tables (static HTML)
 * 
 * @param {string} selector - jQuery selector for the table element
 * @param {object} options - DataTables configuration options (optional)
 * @returns {object|null} - DataTable instance or null if initialization failed
 */
function safeDataTable(selector, options) {
    try {
        if (typeof $.fn.DataTable === 'undefined') {
            console.error('DataTables library is not loaded');
            showErrorAlert('Data table library is not available. Please refresh the page.');
            return null;
        }

        const element = $(selector);
        if (element.length === 0) {
            console.error('DataTable selector not found:', selector);
            showErrorAlert('Data table element not found. Please refresh the page.');
            return null;
        }

        // Destroy existing instance if it exists
        if ($.fn.DataTable.isDataTable(selector)) {
            element.DataTable().destroy();
        }

        // Count columns from thead to ensure proper configuration
        const thead = element.find('thead tr');
        const columnCount = thead.length > 0 ? thead.first().find('th').length : 0;
        
        // Create columnDefs to handle empty/null cells gracefully
        const columnDefs = [];
        for (let i = 0; i < columnCount; i++) {
            columnDefs.push({
                targets: i,
                defaultContent: '',
                render: function (data, type, row) {
                    // Handle null, undefined, or empty data
                    if (data == null || data === '') {
                        return '';
                    }
                    // For display type, return the data as-is (allows HTML)
                    if (type === 'display') {
                        return data;
                    }
                    // For other types (sort, filter, etc.), return text content
                    const $data = $('<div>').html(data);
                    return $data.text() || '';
                }
            });
        }

        const defaultOptions = {
            destroy: true,
            responsive: true,
            processing: true,
            columnDefs: columnDefs,
            language: {
                processing: '<i class="fas fa-spinner fa-spin"></i> Loading...',
                emptyTable: 'No data available',
                loadingRecords: 'Loading...',
                zeroRecords: 'No matching records found',
                error: 'An error occurred while loading data'
            },
            error: function (xhr, error, thrown) {
                console.error('DataTable error:', error, thrown);
                PaykeshoErrorHandler.logError(new Error(`DataTable: ${error} - ${thrown}`), 'datatable');
                showErrorAlert('Failed to load data table. Please refresh the page.');
            }
        };

        const config = Object.assign({}, defaultOptions, options);

        return element.DataTable(config);

    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'datatable-init');
        showErrorAlert('Failed to initialize data table.');
        return null;
    }
}

/**
 * Initialize DataTable safely with enhanced defaults
 * Alias for safeDataTable() for consistency
 * 
 * @param {string} selector - jQuery selector for the table element
 * @param {object} options - DataTables configuration options (optional)
 * @returns {object|null} - DataTable instance or null if initialization failed
 */
function initializeDataTableSafely(selector, options) {
    return safeDataTable(selector, options);
}

// Alert system
function showErrorAlert(message, type = 'error') {
    try {
        const alertClass = type === 'success' ? 'alert-success' :
            type === 'warning' ? 'alert-warning' : 'alert-danger';

        const alertHtml = `
            <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
                <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle'} me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        // Try multiple containers
        const containers = ['#exceptionAlertContainer', '.container-fluid', 'main', 'body'];
        let alertAdded = false;

        for (const container of containers) {
            const $container = $(container);
            if ($container.length > 0) {
                if (container === '#exceptionAlertContainer') {
                    $container.html(alertHtml).show();
                } else {
                    $container.prepend(alertHtml);
                }
                alertAdded = true;
                break;
            }
        }

        if (!alertAdded) {
            // Fallback to browser alert
            alert(message);
        }

        // Auto dismiss after 8 seconds
        setTimeout(function () {
            $('.alert').alert('close');
        }, 8000);

    } catch (error) {
        console.error('Failed to show alert:', error);
        alert(message); // Fallback
    }
}

// Safe element access (jQuery)
function safeElement(selector, callback, fallback) {
    try {
        const element = $(selector);
        if (element.length > 0) {
            return callback(element);
        } else {
            console.warn('Element not found:', selector);
            if (fallback) fallback();
            return null;
        }
    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'element-access');
        if (fallback) fallback();
        return null;
    }
}

// Safe element access (vanilla JS)
function safeQuerySelector(selector, callback, fallback) {
    try {
        const element = document.querySelector(selector);
        if (element) {
            return callback(element);
        } else {
            console.warn('Element not found:', selector);
            if (fallback) fallback();
            return null;
        }
    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'element-access');
        if (fallback) fallback();
        return null;
    }
}

// Safe element access by ID
function safeGetElementById(id, callback, fallback) {
    try {
        const element = document.getElementById(id);
        if (element) {
            return callback(element);
        } else {
            console.warn('Element not found by ID:', id);
            if (fallback) fallback();
            return null;
        }
    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'element-access');
        if (fallback) fallback();
        return null;
    }
}

// Safe form submission
function safeFormSubmit(formSelector, options = {}) {
    try {
        const form = $(formSelector);
        if (form.length === 0) {
            showErrorAlert('Form not found. Please refresh the page.');
            return false;
        }

        // Disable submit button to prevent double submission
        const submitBtn = form.find('button[type="submit"], input[type="submit"]');
        submitBtn.prop('disabled', true);

        // Add loading state
        const originalText = submitBtn.text();
        submitBtn.html('<i class="fas fa-spinner fa-spin me-1"></i>Processing...');

        // Re-enable after timeout
        setTimeout(() => {
            submitBtn.prop('disabled', false).text(originalText);
        }, options.timeout || 10000);

        return true;
    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'form-submit');
        showErrorAlert('Form submission error. Please try again.');
        return false;
    }
}

// Currency formatting with error handling
function safeCurrencyFormat(amount, currency = 'ZMW') {
    try {
        if (amount === null || amount === undefined || amount === '') {
            return '0.00';
        }

        const numAmount = parseFloat(amount);
        if (isNaN(numAmount)) {
            console.warn('Invalid amount for formatting:', amount);
            return '0.00';
        }

        return new Intl.NumberFormat('en-ZM', {
            style: 'currency',
            currency: currency,
            minimumFractionDigits: 2
        }).format(numAmount);

    } catch (error) {
        console.error('Currency formatting error:', error);
        return amount ? amount.toString() : '0.00';
    }
}

// Safe chart initialization
function safeChart(elementId, chartConfig) {
    try {
        const element = document.getElementById(elementId);
        if (!element) {
            console.error('Chart element not found:', elementId);
            return null;
        }

        if (typeof Chart === 'undefined') {
            console.error('Chart.js library not loaded');
            showErrorAlert('Chart library not available. Please refresh the page.');
            return null;
        }

        return new Chart(element, chartConfig);

    } catch (error) {
        PaykeshoErrorHandler.logError(error, 'chart-init');
        console.error('Chart initialization failed:', error);

        // Show placeholder
        const placeholder = document.getElementById(elementId);
        if (placeholder) {
            placeholder.innerHTML = '<div class="text-center text-muted p-4"><i class="fas fa-chart-bar fa-2x mb-2"></i><br>Chart unavailable</div>';
        }

        return null;
    }
}

// Initialize error handling when DOM is ready
document.addEventListener('DOMContentLoaded', function () {
    console.log('Paykesho Error Handling initialized');

    // Add global CSS for error states
    const style = document.createElement('style');
    style.textContent = `
        .error-state {
            background-color: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
            padding: 1rem;
            border-radius: 0.375rem;
            text-align: center;
        }
        
        .loading-state {
            background-color: #e2e3e5;
            border: 1px solid #d6d8db;
            color: #383d41;
            padding: 1rem;
            border-radius: 0.375rem;
            text-align: center;
        }
        
        .error-alert {
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            min-width: 300px;
        }
    `;
    document.head.appendChild(style);
});
