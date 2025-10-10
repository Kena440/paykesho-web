package org.paykesho.paymentservice.payments.repositories;

import org.commons.entity.payments.Payment;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PaymentRepository extends CrudRepository<Payment, Integer> {

    /**
     * Find payment by its reference number (for idempotency checking)
     */
    Optional<Payment> findByPaymentReference(String paymentReference);
}

