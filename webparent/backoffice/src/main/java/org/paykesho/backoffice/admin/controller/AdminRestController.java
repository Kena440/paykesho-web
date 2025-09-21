package org.paykesho.backoffice.admin.controller;

import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.paykesho.backoffice.transaction.service.TransactionsService;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private TransactionsService transactionsService;

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/transactions/summary")
    public Map<String, Object> getTransactionSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            List<Transaction> allTransactions = transactionsService.listAll();

            long totalTransactions = allTransactions.size();
            long settledTransactions = allTransactions.stream()
                    .filter(Transaction::isSettled)
                    .count();
            long unsettledTransactions = allTransactions.stream()
                    .filter(t -> !t.isSettled())
                    .count();

            double totalAmount = allTransactions.stream()
                    .filter(t -> t.getAmount() != null)
                    .mapToDouble(t -> t.getAmount().doubleValue())
                    .sum();

            summary.put("totalTransactions", totalTransactions);
            summary.put("settledTransactions", settledTransactions);
            summary.put("unsettledTransactions", unsettledTransactions);
            summary.put("totalAmount", totalAmount);
            summary.put("settlementRate", totalTransactions > 0 ? (double) settledTransactions / totalTransactions * 100 : 0);

        } catch (Exception e) {
            summary.put("error", "Failed to load transaction summary: " + e.getMessage());
        }

        return summary;
    }

    @GetMapping("/vouchers/summary")
    public Map<String, Object> getVoucherSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            List<Voucher> allVouchers = voucherService.listAll();

            long totalVouchers = allVouchers.size();
            long approvedVouchers = allVouchers.stream()
                    .filter(v -> v.getVoucherStatus() != null && v.getVoucherStatus().name().equals("APPROVED"))
                    .count();
            long pendingVouchers = allVouchers.stream()
                    .filter(v -> v.getVoucherStatus() != null && v.getVoucherStatus().name().equals("PENDING"))
                    .count();
            long rejectedVouchers = allVouchers.stream()
                    .filter(v -> v.getVoucherStatus() != null && v.getVoucherStatus().name().equals("REJECTED"))
                    .count();

            double totalValue = allVouchers.stream()
                    .filter(v -> v.getAmount() != null)
                    .mapToDouble(v -> v.getAmount().doubleValue())
                    .sum();

            summary.put("totalVouchers", totalVouchers);
            summary.put("approvedVouchers", approvedVouchers);
            summary.put("pendingVouchers", pendingVouchers);
            summary.put("rejectedVouchers", rejectedVouchers);
            summary.put("totalValue", totalValue);
            summary.put("approvalRate", totalVouchers > 0 ? (double) approvedVouchers / totalVouchers * 100 : 0);

        } catch (Exception e) {
            summary.put("error", "Failed to load voucher summary: " + e.getMessage());
        }

        return summary;
    }
}
