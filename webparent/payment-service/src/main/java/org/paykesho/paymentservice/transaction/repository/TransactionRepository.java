package org.paykesho.paymentservice.transaction.repository;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    @Query("select count(t.id) as total from Transaction t order by t.txnDate desc")
    Long countAllTransactions();

    @Query("select t from Transaction t where t.merchant.id = ?1 order by t.txnDate desc")
    List<Transaction> getTransactionsByMerchantId(int id);

    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant AND t.settled = false")
    List<Transaction> findUnsettledTransactionsByMerchant(@Param("merchant") Merchant merchant);
}
