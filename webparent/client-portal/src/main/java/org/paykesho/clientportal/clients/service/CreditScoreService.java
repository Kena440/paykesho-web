package org.paykesho.clientportal.clients.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.paykesho.clientportal.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class CreditScoreService {

    // @Autowired
    // private ClientRepository clientRepository; // Unused field
    // @Autowired
    // private CreditScoreRepository creditScoreRepository; // Unused field

    @Autowired
    private VouchersRepository voucherRepository;

    public int calculateCreditScore(Client client) {
        try {
            log.debug("Calculating credit score for client ID: {}", client.getId());
            int score = 850;

            try {
                long delayedPayments = voucherRepository.countDelayedPayments(client.getId());
                score -= (int) (delayedPayments * 10);
                log.debug("Delayed payments: {}, score adjustment: {}", delayedPayments, (int) (delayedPayments * 10));

                BigDecimal totalOutstanding = voucherRepository.getOutstandingBalance(client.getId());
                BigDecimal income = client.getClientIncome();

                if (totalOutstanding == null) {
                    totalOutstanding = BigDecimal.ZERO;
                }

                if (income != null && income.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal debtToIncomeRatio = totalOutstanding.divide(income, 2, RoundingMode.HALF_UP);
                    if (debtToIncomeRatio.compareTo(new BigDecimal("0.5")) > 0) {
                        score -= 50;
                        log.debug("High debt-to-income ratio, score reduced by 50");
                    }
                } else {
                    score -= 50;
                    log.debug("No income data, score reduced by 50");
                }
            } catch (ArithmeticException e) {
                log.error("Arithmetic error while calculating credit score for client {}: {}", client.getId(), e.getMessage());
                score -= 50;
            } catch (Exception e) {
                log.error("Unexpected error while calculating credit score for client {}: {}", client.getId(), e.getMessage());
                score -= 50;
            }

            int finalScore = Math.max(300, score);
            log.debug("Final credit score for client {}: {}", client.getId(), finalScore);
            return finalScore;
        } catch (Exception ex) {
            log.error("Critical error calculating credit score for client {}: {}", client.getId(), ex.getMessage(), ex);
            return 300; // Return minimum score on critical error
        }
    }


}
