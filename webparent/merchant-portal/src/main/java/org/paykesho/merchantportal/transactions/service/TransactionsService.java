package org.paykesho.merchantportal.transactions.service;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.utils.PaykeshoError;
import org.paykesho.merchantportal.transactions.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionsService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Transaction> getMerchantTransactions(Merchant merchant) throws PaykeshoError {
        if (merchant == null) {
            throw new PaykeshoError("no merchant id");
        } else {
            return transactionRepository.getTransactionsByMerchant(merchant);
        }
    }
}
