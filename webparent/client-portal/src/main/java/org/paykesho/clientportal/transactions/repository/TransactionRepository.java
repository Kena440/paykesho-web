package org.paykesho.clientportal.transactions.repository;

import org.commons.entity.Transaction;
import org.commons.entity.client.Client;
import org.paykesho.clientportal.api.dto.TransactionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("select t from Transaction t where t.client = :client order by t.txnDate desc")
    public List<Transaction> getTransactionsByClient(@Param("client") Client client);

    @Query("select count(t.id) as total from Transaction t")
    Long countAllTransactions();

    @Query("select new org.paykesho.clientportal.api.dto.TransactionDto(t.reference, t.merchant.name, t.amount, t.txnDate, t.voucher.reference, t.naration) from Transaction t where t.client = :client")
    List<TransactionDto> getTransactionsByClientFlat(Client client);

    // Additional methods for dashboard analytics
    List<Transaction> findByClient(Client client);

    long countByClient(Client client);

    long countByClientAndSettledTrue(Client client);

    long countByClientAndSettledFalse(Client client);

    long countByClientAndTxnDateAfter(Client client, Date startDate);

    long countByClientAndTxnDateBetween(Client client, Date startDate, Date endDate);

    List<Transaction> findByClientAndTxnDateAfter(Client client, Date startDate);

    List<Transaction> findByClientAndTxnDateBetween(Client client, Date startDate, Date endDate);

    List<Transaction> findByClientAndTxnDateAfterAndSettledTrue(Client client, Date startDate);

    List<Transaction> findByClientAndTxnDateBetweenAndSettledTrue(Client client, Date startDate, Date endDate);
}
