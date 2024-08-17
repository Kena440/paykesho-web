package org.paykesho.backoffice.transaction.service;

import org.commons.entity.Transaction;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class TransactionsService {

    public static final int TRANSACTIONS_PER_PAGE = 10;

    @Autowired
    TransactionRepository transactionRepository;

    /**
     * +
     * This method lists all users in the system
     *
     * @return
     */
    public List<Transaction> listAll() {
        return (List<Transaction>) transactionRepository.findAll(Sort.by("reference").ascending());
    }

    /**
     * +
     * This method lists all users in the system by pagination
     *
     * @return
     */
    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, TRANSACTIONS_PER_PAGE, transactionRepository);
    }

    public Long countAllTransactions() {
        return transactionRepository.countAllTransactions();
    }

    public List<Transaction> getMerchantTransactions(int id) throws PaykeshoError {
        if (id == 0) {
            throw new PaykeshoError("Null parameter id");
        }

        return transactionRepository.getTransactionsByMerchantId(id);
    }
}
