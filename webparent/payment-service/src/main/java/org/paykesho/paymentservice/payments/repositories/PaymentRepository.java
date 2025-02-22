package org.paykesho.paymentservice.payments.repositories;

import org.commons.entity.payments.Payment;
import org.springframework.data.repository.CrudRepository;

public interface PaymentRepository extends CrudRepository<Payment, Integer> {

}

