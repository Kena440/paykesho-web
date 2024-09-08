package org.paykesho.clientportal.transactions.repository;

import org.commons.entity.Transaction;
import org.commons.entity.client.Client;
import org.paykesho.clientportal.api.dto.TransactionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("select t from Transaction t where t.client = :client order by t.txnDate desc")
    public List<Transaction> getTransactionsByClient(@Param("client") Client client);

    @Query("select count(t.id) as total from Transaction t")
    Long countAllTransactions();

    @Query("select new org.paykesho.clientportal.api.dto.TransactionDto(t.reference, t.merchant.name, t.amount, t.txnDate, t.voucher.reference, t.naration) from Transaction t where t.client = :client")
    List<TransactionDto> getTransactionsByClientFlat(Client client);
}
