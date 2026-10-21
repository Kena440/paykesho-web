package org.paykesho.clientportal.dashboard;

import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.client.CreditScore;
import org.paykesho.clientportal.clients.repository.ClientBalanceRepository;
import org.paykesho.clientportal.clients.repository.CreditScoreRepository;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only data for the client dashboard cards. Each call only ever returns
 * the signed-in client's own records.
 */
@RestController
@RequestMapping("/api/client")
public class ClientDashboardApiController {

    private final ClientBalanceRepository clientBalanceRepository;
    private final CreditScoreRepository creditScoreRepository;

    public ClientDashboardApiController(ClientBalanceRepository clientBalanceRepository,
                                        CreditScoreRepository creditScoreRepository) {
        this.clientBalanceRepository = clientBalanceRepository;
        this.creditScoreRepository = creditScoreRepository;
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> balance(@AuthenticationPrincipal ClientUserDetails user) {
        Client client = user.getClient();
        ClientBalance balance = clientBalanceRepository.getClientBalanceByClientId(client.getId());
        BigDecimal available = balance == null || balance.getAvailableBalance() == null
            ? BigDecimal.ZERO : balance.getAvailableBalance();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("availableBalance", available.setScale(2, RoundingMode.HALF_UP));
        body.put("formattedBalance", formatKwacha(available));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/credit-score")
    public ResponseEntity<Map<String, Object>> creditScore(@AuthenticationPrincipal ClientUserDetails user) {
        List<CreditScore> scores = creditScoreRepository.findLatestForClient(user.getClient().getId());
        Map<String, Object> body = new LinkedHashMap<>();
        // No score yet is shown as "N/A" by the dashboard, not as zero.
        body.put("creditScore", scores.isEmpty() ? null : scores.get(0).getScore());
        return ResponseEntity.ok(body);
    }

    /** Same presentation as the format_currency template fragment: "K 1,234.50". */
    static String formatKwacha(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return "K " + format.format(amount.setScale(2, RoundingMode.HALF_UP));
    }
}
