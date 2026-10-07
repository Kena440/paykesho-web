package org.paykesho.clientportal.transactions.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.TransactionDto;
import org.paykesho.clientportal.transactions.repository.TransactionRepository;
import org.paykesho.clientportal.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class TransactionsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private VouchersRepository vouchersRepository;

    public List<Transaction> getClientTransactions(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        }
        return transactionRepository.getTransactionsByClient(client);
    }

    @Transactional
    public Transaction save(Transaction transaction) throws PaykeshoError {
        if (transaction == null) {
            throw new PaykeshoError("no transaction");
        }
        return transactionRepository.save(transaction);
    }

    /**
     * Atomically consumes e-value from the specific approved voucher.
     * The voucher row is locked for the duration of the transaction so two
     * simultaneous QR payments cannot spend the same e-value.
     */
    @Transactional
    public Transaction savePaymentAndConsumeVoucher(Transaction transaction, Integer voucherId, BigDecimal amount)
            throws PaykeshoError {

        if (transaction == null || transaction.getClient() == null) {
            throw new PaykeshoError("Client transaction is required");
        }
        if (voucherId == null || amount == null || amount.signum() <= 0) {
            throw new PaykeshoError("Invalid payment amount or voucher");
        }

        Voucher voucher = vouchersRepository.findByIdForUpdate(voucherId);
        if (voucher == null || !voucher.isApproved() || voucher.getAvailableAmount() == null) {
            throw new PaykeshoError("Voucher is not available for payment");
        }
        if (!voucher.getClient().getId().equals(transaction.getClient().getId())) {
            throw new PaykeshoError("Voucher does not belong to this client");
        }
        if (voucher.getAvailableAmount().compareTo(amount) < 0) {
            throw new PaykeshoError("Insufficient voucher balance");
        }

        voucher.setAvailableAmount(voucher.getAvailableAmount().subtract(amount));
        vouchersRepository.save(voucher);

        transaction.setVoucher(voucher);
        return transactionRepository.save(transaction);
    }

    public List<TransactionDto> getClientTransactionsFlat(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client");
        }
        return transactionRepository.getTransactionsByClientFlat(client);
    }
}
