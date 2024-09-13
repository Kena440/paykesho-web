/*
package org.paykesho.backoffice.client.service;

import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.repayments.repository.PayrollDeductionRepository;
import org.paykesho.backoffice.repayments.repository.RepaymentScheduleRepository;
import org.paykesho.backoffice.transaction.repository.TransactionRepository;
import org.paykesho.backoffice.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class CreditRatingService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;

    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;

    @Autowired
    private VouchersRepository vouchersRepository;

    public int calculateCreditScore(Integer clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client ID"));

        int score = 0;

        score += calculatePaymentHistoryScore(client);
        score += calculateOutstandingDebtScore(client);
        score += calculateCreditHistoryLengthScore(client);
        score += calculateCreditMixScore(client);
        score += calculateNewCreditScore(client);

        return score;
    }

    private int calculatePaymentHistoryScore(Client client) {
        // Retrieve transactions for the client
        List<PayrollDeduction> transactions = payrollDeductionRepository.findByClientOrganizationId(client.getId());

        if (transactions.isEmpty()) {
            return 0; // No payment history
        }

        // Example: 100 points if all payments are on time
        long onTimePayments = transactions.stream()
                .filter(Transaction::isOnTime)
                .count();

        double paymentHistoryScore = ((double) onTimePayments / transactions.size()) * 100;
        return (int) paymentHistoryScore;
    }

    private int calculateOutstandingDebtScore(Client client) {
        // Retrieve repayment schedules for the client
        List<PayrollDeduction> payrollDeductions = payrollDeductionRepository.findByClientId(client.getId());

        BigDecimal totalDebt = payrollDeductions.stream()
                .mapToDouble(PayrollDeduction::getAmountDeducted)
                .sum();

        double paidAmount = client.getPaidAmount(); // Assuming `getPaidAmount` gives total paid amount

        if (totalDebt <= 0) {
            return 100; // No debt means perfect score
        }

        double debtRatio = paidAmount / totalDebt;
        return (int) (debtRatio * 100);
    }

    private int calculateCreditHistoryLengthScore(Client client) {
        Date creditStartDate = client.getCreatedTime();
        if (creditStartDate == null) {
            return 0; // No credit history
        }

        long years = ChronoUnit.YEARS.between((Temporal) creditStartDate, LocalDateTime.now());
        return (int) Math.min(years * 10, 100); // Max score of 100
    }

    private int calculateCreditMixScore(Client client) {
        // Retrieve vouchers for the client
        List<Voucher> vouchers = vouchersRepository.getVouchersByClient(client.getId());

        int voucherCount = vouchers.size();
        if (voucherCount == 0) {
            return 0; // No credit mix
        }

        // Example: 10 points per voucher type, capped at 50
        return Math.min(voucherCount * 10, 50);
    }

    private int calculateNewCreditScore(Client client) {
        // Retrieve repayment schedules for the client
        List<RepaymentSchedule> repaymentSchedules = repaymentScheduleRepository.findByClientId(client.getId());

        int recentApplications = repaymentSchedules.size(); // Assuming each schedule is a new application
        return Math.max(0, 50 - (recentApplications * 5));
    }
}
*/
