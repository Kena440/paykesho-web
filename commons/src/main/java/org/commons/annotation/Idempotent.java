package org.commons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark endpoints/methods as idempotent.
 * When applied, the framework will automatically handle duplicate requests
 * by checking for existing idempotency records and replaying responses.
 *
 * Usage:
 * <pre>
 * {@literal @}PostMapping("/payments")
 * {@literal @}Idempotent(keyHeader = "Idempotency-Key", required = true, ttlHours = 24)
 * public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request,
 *                                        @RequestHeader("Idempotency-Key") String idempotencyKey) {
 *     // ... payment processing
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    /**
     * The name of the header containing the idempotency key
     * Default: "Idempotency-Key"
     */
    String keyHeader() default "Idempotency-Key";
    
    /**
     * Whether the idempotency key is required
     * If true, requests without the key will be rejected with 400 Bad Request
     * If false, a key will be auto-generated for the request
     */
    boolean required() default true;
    
    /**
     * Time-to-live for the idempotency record in hours
     * Default: 24 hours
     */
    int ttlHours() default 24;
    
    /**
     * Whether to store the request body
     * Set to false for large payloads to save space
     */
    boolean storeRequest() default true;
    
    /**
     * Whether to store the response body for replay
     * Set to false for large responses to save space
     */
    boolean storeResponse() default true;
    
    /**
     * Custom identifier for grouping related operations
     * Can use SpEL expressions: e.g., "#paymentRequest.userId"
     */
    String groupBy() default "";
    
    /**
     * Description of the operation for logging/monitoring
     */
    String description() default "";
}

