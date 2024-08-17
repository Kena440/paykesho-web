package org.paykesho.clientportal.transactions.service;

import org.commons.entity.Transaction;
import org.commons.entity.client.Client;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.TransactionDto;
import org.paykesho.clientportal.transactions.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionsService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Transaction> getClientTransactions(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return transactionRepository.getTransactionsByClient(client);
        }
    }

    public Transaction save(Transaction transaction) throws PaykeshoError {
        if (transaction == null) {
            throw new PaykeshoError("no transaction");
        } else {
            return transactionRepository.save(transaction);
        }
    }

    public List<TransactionDto> getClientTransactionsFlat(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client");
        } else {
            return transactionRepository.getTransactionsByClientFlat(client);
        }
    }
}
