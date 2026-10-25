package org.paykesho.merchantportal.dashboard;

import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.paykesho.merchantportal.merchants.repository.MerchantRepository;
import org.paykesho.merchantportal.security.MerchantAdminUserDetails;
import org.paykesho.merchantportal.transactions.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only data for the merchant dashboard cards, limited to the merchant the
 * signed-in admin is the contact person for.
 */
@RestController
@RequestMapping("/api")
public class MerchantDashboardApiController {

    private static final ZoneId LUSAKA = ZoneId.of("Africa/Lusaka");

    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;

    public MerchantDashboardApiController(MerchantRepository merchantRepository,
                                          TransactionRepository transactionRepository) {
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/merchant/info")
    public ResponseEntity<Map<String, Object>> merchantInfo(@AuthenticationPrincipal MerchantAdminUserDetails user) {
        Merchant merchant = merchantFor(user);
        if (merchant == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", merchant.getCode());
        body.put("name", merchant.getName());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/transactions/summary")
    public ResponseEntity<Map<String, Object>> transactionSummary(@AuthenticationPrincipal MerchantAdminUserDetails user) {
        Merchant merchant = merchantFor(user);
        if (merchant == null) {
            return ResponseEntity.notFound().build();
        }
        List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
        YearMonth thisMonth = YearMonth.now(LUSAKA);
        long current = countInMonth(transactions, thisMonth);
        long previous = countInMonth(transactions, thisMonth.minusMonths(1));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalTransactions", transactions.size());
        body.put("transactionsThisMonth", current);
        // Month-on-month change in transaction count; 0 when there is no prior month to compare.
        body.put("percentageChange", previous == 0 ? 0 : Math.round((current - previous) * 100.0 / previous));
        return ResponseEntity.ok(body);
    }

    private Merchant merchantFor(MerchantAdminUserDetails user) {
        if (user == null || user.getMerchantAdmin() == null) {
            return null;
        }
        return merchantRepository.getMerchantByContactPersonId(user.getMerchantAdmin().getId());
    }

    private static long countInMonth(List<Transaction> transactions, YearMonth month) {
        return transactions.stream()
            .filter(t -> t.getTxnDate() != null)
            .filter(t -> YearMonth.from(LocalDate.ofInstant(java.time.Instant.ofEpochMilli(t.getTxnDate().getTime()), LUSAKA)).equals(month))
            .count();
    }
}
