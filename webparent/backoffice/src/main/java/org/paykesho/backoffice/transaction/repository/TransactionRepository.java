package org.paykesho.backoffice.transaction.repository;

import org.commons.entity.Transaction;
import org.commons.entity.client.Client;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends SearchRepository<Transaction, Integer> {

    @Query("select t from Transaction t where t.client.id = :client")
    public Client getTransactionByClient(@Param("client") Integer client);

    public Long countById(Integer id);

    @Query("select t from Transaction t where concat(t.client, ' ', t.merchant.name, ' ', t.reference ) like %?1%")
    public Page<Transaction> findAll(String keyword, Pageable pageable);

    @Query("select count(t.id) as total from Transaction t")
    Long countAllTransactions();

    @Query("select t from Transaction t where t.merchant.id = ?1")
    List<Transaction> getTransactionsByMerchantId(int id);
}
