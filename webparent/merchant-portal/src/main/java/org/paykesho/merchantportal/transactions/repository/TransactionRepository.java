package org.paykesho.merchantportal.transactions.repository;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("select t from Transaction t where t.merchant = :merchant order by t.txnDate desc")
    public List<Transaction> getTransactionsByMerchant(@Param("merchant") Merchant merchant);

    @Query("select count(t.id) as total from Transaction t where t.merchant = :merchant")
    Long countAllTransactions(@Param("merchant") Merchant merchant);

    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant AND t.settled = false")
    List<Transaction> findUnsettledTransactionsByMerchant(@Param("merchant") Merchant merchant);

}
