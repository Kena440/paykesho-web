package org.paykesho.paymentservice.repository;

import org.commons.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByReference(String reference);
    List<Transaction> findBySettlementStatus(String settlementStatus);
}
