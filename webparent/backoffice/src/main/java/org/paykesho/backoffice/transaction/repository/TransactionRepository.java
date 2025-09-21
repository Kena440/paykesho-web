package org.paykesho.backoffice.transaction.repository;

import org.commons.entity.Transaction;
import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface TransactionRepository extends SearchRepository<Transaction, Integer> {

    @Query("select t from Transaction t where t.client.id = :client order by t.txnDate desc")
    public Client getTransactionByClient(@Param("client") Integer client);

    public Long countById(Integer id);

    @Query("select t from Transaction t where concat(t.client, ' ', t.merchant.name, ' ', t.reference ) like %?1%")
    public Page<Transaction> findAll(String keyword, Pageable pageable);

    @Query("select count(t.id) as total from Transaction t order by t.txnDate desc")
    Long countAllTransactions();

    @Query("select t from Transaction t where t.merchant.id = ?1 order by t.txnDate desc")
    List<Transaction> getTransactionsByMerchantId(int id);

    @Query("SELECT t FROM Transaction t WHERE t.settled = false")
    List<Transaction> findUnsettledTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant AND t.settled = false")
    List<Transaction> findUnsettledTransactionsByMerchant(@Param("merchant") Merchant merchant);

    long countByTxnDateAfter(Date date);

    // Additional methods for dashboard analytics
    long countBySettledTrue();

    long countBySettledFalse();

    long countByMerchantId(Integer merchantId);

    long countByMerchantIdAndSettledTrue(Integer merchantId);

    long countByMerchantIdAndSettledFalse(Integer merchantId);

    long countByClientId(Integer clientId);

    long countByClientIdAndSettledTrue(Integer clientId);

    long countByClientIdAndSettledFalse(Integer clientId);

    List<Transaction> findByMerchantId(Integer merchantId);

    List<Transaction> findByClientId(Integer clientId);

    List<Transaction> findByTxnDateAfterAndSettledTrue(Date date);

    List<Transaction> findByTxnDateBetweenAndSettledTrue(Date startDate, Date endDate);

    @Query("SELECT t.merchant.id, t.merchant.name, COUNT(t) as transactionCount " +
            "FROM Transaction t " +
            "GROUP BY t.merchant.id, t.merchant.name " +
            "ORDER BY transactionCount DESC")
    List<Object[]> findTopMerchantsByTransactionCount(int limit);

    @Query("SELECT t.merchant.id, t.merchant.name, SUM(t.amount) as totalValue " +
            "FROM Transaction t " +
            "WHERE t.settled = true " +
            "GROUP BY t.merchant.id, t.merchant.name " +
            "ORDER BY totalValue DESC")
    List<Object[]> findTopMerchantsByTransactionValue(int limit);

}
