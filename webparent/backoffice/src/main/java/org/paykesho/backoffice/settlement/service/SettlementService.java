package org.paykesho.backoffice.settlement.service;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.commons.enums.SettlementStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.settlement.repository.SettlementRepository;
import org.paykesho.backoffice.transaction.repository.TransactionRepository;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SettlementService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;

    public List<Settlement> listAll() {
        return settlementRepository.listAll();
    }

    public List<Settlement> listAll(Merchant merchant) {
        return settlementRepository.getSettlementsByMerchant(merchant);
    }

    public void reconcileAndSettleForMerchant(Merchant merchant) throws PaykeshoError {
        List<Transaction> unsettledTransactions = transactionRepository.findUnsettledTransactionsByMerchant(merchant);

        BigDecimal totalAmount = unsettledTransactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_STL");
            if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

            Settlement settlement = new Settlement();
            settlement.setReference(paykeshoRef);
            settlement.setMerchant(merchant);
            settlement.setSettlementAmount(totalAmount);
            settlement.setSettlementStatus(SettlementStatus.PENDING);
            settlementRepository.save(settlement);

            boolean paymentSuccessful = initiatePayment(merchant, totalAmount);

            if (paymentSuccessful) {
                settlement.setSettlementStatus(SettlementStatus.COMPLETED);
                markTransactionsAsSettled(unsettledTransactions);
            } else {
                settlement.setSettlementStatus(SettlementStatus.FAILED);
            }

            settlementRepository.save(settlement);
        }
    }

    private boolean initiatePayment(Merchant merchant, BigDecimal amount) {
        // Simulate payment processing (bank or mobile money transfer)
        // return true if successful, false if failed
        return false; // Assuming payment is successful for now
    }

    private void markTransactionsAsSettled(List<Transaction> transactions) {
        for (Transaction txn : transactions) {
            txn.setSettled(true);
            transactionRepository.save(txn);
        }
    }
}
