/**
 * Paykesho Unified Toast Notification System
 * Provides consistent toast notifications across all modules
 * Version: 1.0.0
 */

(function (window) {
    'use strict';

    /**
     * Toast configuration
     */
    const TOAST_CONFIG = {
        duration: 5000,        // Default duration in ms
        position: 'top-end',   // Position: top-end, top-start, bottom-end, bottom-start
        maxToasts: 5,          // Maximum number of toasts to show at once
        animation: true        // Enable/disable animation
    };

    /**
     * Toast types with their respective styling
     */
    const TOAST_TYPES = {
        success: {
            bgClass: 'bg-success',
            icon: 'fa-check-circle',
            title: 'Success'
        },
        error: {
            bgClass: 'bg-danger',
            icon: 'fa-exclamation-circle',
            title: 'Error'
        },
        warning: {
            bgClass: 'bg-warning',
            icon: 'fa-exclamation-triangle',
            title: 'Warning'
        },
        info: {
            bgClass: 'bg-info',
            icon: 'fa-info-circle',
            title: 'Information'
        }
    };

    /**
     * Main Toast Service
     */
    const PaykeshoToast = {
        container: null,
        toasts: [],

        /**
         * Initialize toast container
         */
        init: function () {
            if (this.container) return;

            // Create toast container
            const positionClass = this.getPositionClass(TOAST_CONFIG.position);
            this.container = document.createElement('div');
            this.container.id = 'paykesho-toast-container';
            this.container.className = `toast-container position-fixed ${positionClass} p-3`;
            this.container.style.zIndex = '9999';
            document.body.appendChild(this.container);

            console.log('Paykesho Toast System initialized');
        },

        /**
         * Get position class from position string
         */
        getPositionClass: function (position) {
            const positions = {
                'top-end': 'top-0 end-0',
                'top-start': 'top-0 start-0',
                'bottom-end': 'bottom-0 end-0',
                'bottom-start': 'bottom-0 start-0',
                'top-center': 'top-0 start-50 translate-middle-x',
                'bottom-center': 'bottom-0 start-50 translate-middle-x'
            };
            return positions[position] || positions['top-end'];
        },

        /**
         * Show a toast notification
         * @param {string} message - The message to display
         * @param {string} type - Type: success, error, warning, info
         * @param {object} options - Additional options
         */
        show: function (message, type = 'info', options = {}) {
            this.init();

            // Validate type
            if (!TOAST_TYPES[type]) {
                console.warn(`Invalid toast type: ${type}. Using 'info' instead.`);
                type = 'info';
            }

            const config = TOAST_TYPES[type];
            const duration = options.duration || TOAST_CONFIG.duration;
            const title = options.title || config.title;
            const icon = options.icon || config.icon;

            // Check max toasts limit
            if (this.toasts.length >= TOAST_CONFIG.maxToasts) {
                this.removeOldest();
            }

            // Create toast element
            const toastId = 'toast-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
            const toastHtml = this.createToastHTML(toastId, message, title, icon, config.bgClass, options);

            // Add to container
            this.container.insertAdjacentHTML('beforeend', toastHtml);

            // Get the toast element
            const toastElement = document.getElementById(toastId);
            if (!toastElement) {
                console.error('Failed to create toast element');
                return;
            }

            // Initialize Bootstrap toast
            let bsToast;
            if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
                bsToast = new bootstrap.Toast(toastElement, {
                    autohide: !options.persistent,
                    delay: duration
                });
            } else {
                console.warn('Bootstrap Toast not available, using fallback');
                this.fallbackToast(message, type);
                return;
            }

            // Track toast
            this.toasts.push({
                id: toastId,
                element: toastElement,
                bsToast: bsToast,
                timestamp: Date.now()
            });

            // Show toast
            bsToast.show();

            // Remove from tracking when hidden
            toastElement.addEventListener('hidden.bs.toast', () => {
                this.removeToast(toastId);
            });

            // Auto-remove if persistent option is set
            if (options.persistent && options.persistDuration) {
                setTimeout(() => {
                    this.removeToast(toastId);
                }, options.persistDuration);
            }

            return toastId;
        },

        /**
         * Create toast HTML
         */
        createToastHTML: function (id, message, title, icon, bgClass, options) {
            const hasAction = options.actionText && options.actionCallback;
            const progressBar = options.showProgress ? `
                <div class="toast-progress-bar">
                    <div class="toast-progress-fill" style="animation-duration: ${options.duration || TOAST_CONFIG.duration}ms"></div>
                </div>
            ` : '';

            return `
                <div id="${id}" class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                    <div class="d-flex">
                        <div class="toast-body d-flex align-items-center">
                            ${icon ? `<i class="fas ${icon} me-2"></i>` : ''}
                            <div class="flex-grow-1">
                                ${title ? `<strong class="d-block mb-1">${this.escapeHtml(title)}</strong>` : ''}
                                <span>${this.escapeHtml(message)}</span>
                                ${hasAction ? `
                                    <button type="button" class="btn btn-sm btn-light mt-2" onclick="window.PaykeshoToast.handleAction('${id}')">
                                        ${this.escapeHtml(options.actionText)}
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                    </div>
                    ${progressBar}
                </div>
            `;
        },

        /**
         * Escape HTML to prevent XSS
         */
        escapeHtml: function (text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        },

        /**
         * Handle action button click
         */
        handleAction: function (toastId) {
            const toast = this.toasts.find(t => t.id === toastId);
            if (toast && toast.actionCallback) {
                toast.actionCallback();
                this.removeToast(toastId);
            }
        },

        /**
         * Remove oldest toast
         */
        removeOldest: function () {
            if (this.toasts.length === 0) return;

            const oldest = this.toasts[0];
            this.removeToast(oldest.id);
        },

        /**
         * Remove a specific toast
         */
        removeToast: function (toastId) {
            const index = this.toasts.findIndex(t => t.id === toastId);
            if (index === -1) return;

            const toast = this.toasts[index];

            // Hide and remove
            if (toast.bsToast) {
                toast.bsToast.hide();
            }

            setTimeout(() => {
                if (toast.element && toast.element.parentNode) {
                    toast.element.parentNode.removeChild(toast.element);
                }
                this.toasts.splice(index, 1);
            }, 300);
        },

        /**
         * Clear all toasts
         */
        clearAll: function () {
            this.toasts.forEach(toast => {
                if (toast.bsToast) {
                    toast.bsToast.hide();
                }
                if (toast.element && toast.element.parentNode) {
                    toast.element.parentNode.removeChild(toast.element);
                }
            });
            this.toasts = [];
        },

        /**
         * Fallback for when Bootstrap is not available
         */
        fallbackToast: function (message, type) {
            const alertType = type === 'error' ? 'danger' : type;
            const alertHtml = `
                <div class="alert alert-${alertType} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3" 
                     style="z-index: 9999; min-width: 300px;" role="alert">
                    ${this.escapeHtml(message)}
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            `;
            document.body.insertAdjacentHTML('beforeend', alertHtml);
        },

        // Convenience methods
        success: function (message, options = {}) {
            return this.show(message, 'success', options);
        },

        error: function (message, options = {}) {
            return this.show(message, 'error', options);
        },

        warning: function (message, options = {}) {
            return this.show(message, 'warning', options);
        },

        info: function (message, options = {}) {
            return this.show(message, 'info', options);
        },

        /**
         * Show toast from backend error response
         */
        fromError: function (error, fallbackMessage = 'An error occurred') {
            let message = fallbackMessage;
            let type = 'error';

            // Handle different error formats
            if (typeof error === 'string') {
                message = error;
            } else if (error.message) {
                message = error.message;
            } else if (error.responseJSON) {
                message = error.responseJSON.message || error.responseJSON.errorMessage || fallbackMessage;
                type = error.responseJSON.errorCode ? 'warning' : 'error';
            } else if (error.responseText) {
                try {
                    const parsed = JSON.parse(error.responseText);
                    message = parsed.message || parsed.errorMessage || fallbackMessage;
                } catch (e) {
                    message = error.responseText.substring(0, 200);
                }
            }

            return this.show(message, type);
        },

        /**
         * Show loading toast with progress
         */
        loading: function (message = 'Loading...', options = {}) {
            return this.show(message, 'info', {
                ...options,
                persistent: true,
                icon: 'fa-spinner fa-spin'
            });
        },

        /**
         * Update an existing toast
         */
        update: function (toastId, message, type) {
            const toast = this.toasts.find(t => t.id === toastId);
            if (!toast) return;

            const bodyElement = toast.element.querySelector('.toast-body span');
            if (bodyElement) {
                bodyElement.textContent = message;
            }

            if (type && TOAST_TYPES[type]) {
                // Remove old background class
                Object.values(TOAST_TYPES).forEach(t => {
                    toast.element.classList.remove(t.bgClass);
                });
                // Add new background class
                toast.element.classList.add(TOAST_TYPES[type].bgClass);
            }
        }
    };

    // Make available globally
    window.PaykeshoToast = PaykeshoToast;

    // Backward compatibility aliases
    window.showToast = (message, type, options) => PaykeshoToast.show(message, type, options);
    window.showToastMessage = (message) => PaykeshoToast.info(message);
    window.showSuccessToast = (message) => PaykeshoToast.success(message);
    window.showErrorToast = (message) => PaykeshoToast.error(message);
    window.showWarningToast = (message) => PaykeshoToast.warning(message);

    // Auto-initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => PaykeshoToast.init());
    } else {
        PaykeshoToast.init();
    }

    // Add CSS for progress bar
    const style = document.createElement('style');
    style.textContent = `
        .toast-progress-bar {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 3px;
            background-color: rgba(255, 255, 255, 0.3);
            overflow: hidden;
        }
        
        .toast-progress-fill {
            height: 100%;
            background-color: rgba(255, 255, 255, 0.8);
            animation: toast-progress linear forwards;
        }
        
        @keyframes toast-progress {
            from { width: 100%; }
            to { width: 0%; }
        }
        
        .toast {
            min-width: 250px;
            max-width: 400px;
        }
        
        .toast-body {
            padding: 0.75rem;
        }
    `;
    document.head.appendChild(style);

})(window);

