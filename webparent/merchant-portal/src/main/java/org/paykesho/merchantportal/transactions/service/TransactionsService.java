package org.paykesho.merchantportal.transactions.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.commons.utils.PaykeshoError;
import org.paykesho.merchantportal.transactions.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TransactionsService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Transaction> getMerchantTransactions(Merchant merchant) throws PaykeshoError {
        try {
            log.debug("Retrieving transactions for merchant ID: {}", merchant != null ? merchant.getId() : "null");
            if (merchant == null) {
                throw new PaykeshoError("no merchant id");
            } else {
                List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
                log.debug("Successfully retrieved {} transactions for merchant ID: {}", transactions.size(), merchant.getId());
                return transactions;
            }
        } catch (PaykeshoError ex) {
            log.warn("PaykeshoError retrieving merchant transactions: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error retrieving transactions for merchant ID {}: {}", merchant != null ? merchant.getId() : "null", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve merchant transactions", ex);
        }
    }

    public Map<String, Object> getTransactionStats() {
        try {
            log.debug("Calculating transaction statistics");
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            long totalTransactions = transactionRepository.count();
            long transactionsLastWeek = transactionRepository.countByTxnDateAfter(oneWeekAgoDate);

            double percentageChange = 0.0;
            if (totalTransactions > transactionsLastWeek) {
                percentageChange = ((double) transactionsLastWeek / (totalTransactions - transactionsLastWeek)) * 100;
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTransactions", totalTransactions);
            stats.put("txnPercentageChange", String.format("%.2f", percentageChange));

            log.debug("Transaction stats - Total: {}, Last week: {}, Percentage change: {}",
                    totalTransactions, transactionsLastWeek, percentageChange);
            return stats;
        } catch (Exception ex) {
            log.error("Error calculating transaction statistics: {}", ex.getMessage(), ex);
            // Return default stats on error
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalTransactions", 0L);
            defaultStats.put("txnPercentageChange", "0.00");
            return defaultStats;
        }
    }
}
