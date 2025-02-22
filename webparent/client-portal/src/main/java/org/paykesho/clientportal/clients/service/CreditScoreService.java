package org.paykesho.clientportal.clients.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.paykesho.clientportal.clients.repository.ClientRepository;
import org.paykesho.clientportal.clients.repository.CreditScoreRepository;
import org.paykesho.clientportal.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class CreditScoreService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CreditScoreRepository creditScoreRepository;

    @Autowired
    private VouchersRepository voucherRepository;

    public int calculateCreditScore(Client client) {
        int score = 850;

        try {
            long delayedPayments = voucherRepository.countDelayedPayments(client.getId());
            score -= (int) (delayedPayments * 10);

            BigDecimal totalOutstanding = voucherRepository.getOutstandingBalance(client.getId());
            BigDecimal income = client.getClientIncome();

            if (totalOutstanding == null) {
                totalOutstanding = BigDecimal.ZERO;
            }

            if (income != null && income.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal debtToIncomeRatio = totalOutstanding.divide(income, 2, RoundingMode.HALF_UP);
                if (debtToIncomeRatio.compareTo(new BigDecimal("0.5")) > 0) {
                    score -= 50;
                }
            } else {
                score -= 50;
            }
        } catch (ArithmeticException e) {
            log.error("Arithmetic error while calculating credit score: " + e.getMessage());
            score -= 50;
        } catch (Exception e) {
            log.error("Unexpected error while calculating credit score: " + e.getMessage());
            score -= 50;
        }

        return Math.max(300, score);
    }


}
