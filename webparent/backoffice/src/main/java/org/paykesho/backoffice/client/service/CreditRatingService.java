/*
package org.paykesho.backoffice.client.service;

import org.commons.entity.client.Client;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.transaction.repository.TransactionRepository;
import org.paykesho.backoffice.voucher.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class CreditRatingService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    public int calculateCreditScore(Integer clientId) {
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new IllegalArgumentException("Invalid client ID"));

        int score = 0;

        score += calculatePaymentHistoryScore(client);

        score += calculateOutstandingDebtScore(client);

        score += calculateCreditHistoryLengthScore(client);

        score += calculateCreditMixScore(client);

        score += calculateNewCreditScore(client);

        return score;
    }

    private int calculatePaymentHistoryScore(Client client) {
        // Implement payment history logic
        // Example: 100 points if all payments are on time
        return 100;
    }

    private int calculateOutstandingDebtScore(Client client) {
        // Implement outstanding debt logic
        // Example: Deduct points based on outstanding debt percentage
        return (int) ((client.getPaidAmount() / client.getTotalDebt()) * 100);
    }

    private int calculateCreditHistoryLengthScore(Client client) {
        // Implement length of credit history logic
        // Example: More points for a longer history
        long years = ChronoUnit.YEARS.between(client.getCreditStartDate(), LocalDateTime.now());
        return (int) (years * 10);
    }

    private int calculateCreditMixScore(Client client) {
        // Implement credit mix logic
        // Example: Points based on diversity of credit types
        return 50; // Example value
    }

    private int calculateNewCreditScore(Client client) {
        // Implement new credit logic
        // Example: Deduct points for recent credit applications
        return Math.max(0, 50 - (client.getCreditApplications() * 10));
    }
}
*/
