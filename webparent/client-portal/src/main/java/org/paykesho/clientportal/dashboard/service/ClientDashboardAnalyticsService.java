package org.paykesho.clientportal.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;
import org.paykesho.clientportal.transactions.repository.TransactionRepository;
import org.paykesho.clientportal.voucher.repository.VouchersRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClientDashboardAnalyticsService {

    private final TransactionRepository transactionRepository;
    private final VouchersRepository vouchersRepository;
    // private final ClientBalanceRepository clientBalanceRepository; // Unused field

    /**
     * Get comprehensive client-specific analytics
     */
    public Map<String, Object> getClientAnalytics(Client client) {
        try {
            log.debug("Generating analytics for client: {}", client != null ? client.getName() : "null");

            if (client == null) {
                log.warn("Client is null, returning default analytics");
                return getDefaultClientAnalytics();
            }

            Map<String, Object> analytics = new HashMap<>();

            // Basic metrics
            analytics.put("totalTransactions", getClientTransactionCount(client));
            analytics.put("successfulTransactions", getClientSuccessfulTransactions(client));
            analytics.put("failedTransactions", getClientFailedTransactions(client));
            analytics.put("successRate", getClientSuccessRate(client));
            analytics.put("totalSpent", getClientTotalSpent(client));
            analytics.put("averageTransactionValue", getClientAverageTransactionValue(client));

            // Voucher metrics
            analytics.put("totalVouchers", getClientVoucherCount(client));
            analytics.put("approvedVouchers", getClientApprovedVouchers(client));
            analytics.put("pendingVouchers", getClientPendingVouchers(client));
            analytics.put("voucherApprovalRate", getClientVoucherApprovalRate(client));
            analytics.put("totalVoucherValue", getClientTotalVoucherValue(client));

            // Time-based metrics
            analytics.put("todayTransactions", getClientTodayTransactions(client));
            analytics.put("todaySpent", getClientTodaySpent(client));
            analytics.put("weeklyTransactions", getClientWeeklyTransactions(client));
            analytics.put("weeklySpent", getClientWeeklySpent(client));
            analytics.put("monthlyTransactions", getClientMonthlyTransactions(client));
            analytics.put("monthlySpent", getClientMonthlySpent(client));

            // Merchant analytics
            analytics.put("uniqueMerchants", getClientUniqueMerchants(client));
            analytics.put("topMerchants", getClientTopMerchants(client, 5));
            analytics.put("merchantUsageBreakdown", getClientMerchantUsageBreakdown(client));

            // Trends
            analytics.put("spendingTrend", getClientSpendingTrend(client, 30));
            analytics.put("transactionTrend", getClientTransactionTrend(client, 30));
            analytics.put("voucherTrend", getClientVoucherTrend(client, 12));

            // Performance metrics
            analytics.put("lastTransactionDate", getClientLastTransactionDate(client));
            analytics.put("transactionFrequency", getClientTransactionFrequency(client));
            analytics.put("averageDaysBetweenTransactions", getClientAverageDaysBetweenTransactions(client));

            log.debug("Successfully generated client analytics for: {}", client.getName());
            return analytics;

        } catch (Exception e) {
            log.error("Error generating client analytics for client {}: {}",
                    client != null ? client.getName() : "null", e.getMessage(), e);
            return getDefaultClientAnalytics();
        }
    }

    // ========== BASIC METRICS ==========

    private Long getClientTransactionCount(Client client) {
        try {
            return transactionRepository.countByClient(client);
        } catch (Exception e) {
            log.warn("Error getting client transaction count for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getClientSuccessfulTransactions(Client client) {
        try {
            return transactionRepository.countByClientAndSettledTrue(client);
        } catch (Exception e) {
            log.warn("Error getting client successful transactions for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getClientFailedTransactions(Client client) {
        try {
            return transactionRepository.countByClientAndSettledFalse(client);
        } catch (Exception e) {
            log.warn("Error getting client failed transactions for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private Double getClientSuccessRate(Client client) {
        try {
            long total = getClientTransactionCount(client);
            long successful = getClientSuccessfulTransactions(client);
            if (total == 0) return 0.0;
            return (double) successful / total * 100;
        } catch (Exception e) {
            log.warn("Error calculating client success rate for client {}: {}", client.getName(), e.getMessage());
            return 0.0;
        }
    }

    private BigDecimal getClientTotalSpent(Client client) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            return transactions.stream()
                    .filter(Transaction::isSettled)
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting client total spent for client {}: {}", client.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getClientAverageTransactionValue(Client client) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            if (transactions.isEmpty()) return BigDecimal.ZERO;

            BigDecimal totalValue = transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return totalValue.divide(new BigDecimal(transactions.size()), 2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error getting client average transaction value for client {}: {}", client.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== VOUCHER METRICS ==========

    private Long getClientVoucherCount(Client client) {
        try {
            List<Voucher> vouchers = vouchersRepository.getVouchersByClient(client);
            return (long) vouchers.size();
        } catch (Exception e) {
            log.warn("Error getting client voucher count for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getClientApprovedVouchers(Client client) {
        try {
            List<Voucher> vouchers = vouchersRepository.getVouchersByClient(client);
            return vouchers.stream()
                    .filter(Voucher::isApproved)
                    .count();
        } catch (Exception e) {
            log.warn("Error getting client approved vouchers for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getClientPendingVouchers(Client client) {
        try {
            List<Voucher> vouchers = vouchersRepository.getVouchersByClient(client);
            return vouchers.stream()
                    .filter(voucher -> !voucher.isApproved())
                    .count();
        } catch (Exception e) {
            log.warn("Error getting client pending vouchers for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private Double getClientVoucherApprovalRate(Client client) {
        try {
            long totalVouchers = getClientVoucherCount(client);
            long approvedVouchers = getClientApprovedVouchers(client);
            if (totalVouchers == 0) return 0.0;
            return (double) approvedVouchers / totalVouchers * 100;
        } catch (Exception e) {
            log.warn("Error calculating client voucher approval rate for client {}: {}", client.getName(), e.getMessage());
            return 0.0;
        }
    }

    private BigDecimal getClientTotalVoucherValue(Client client) {
        try {
            List<Voucher> vouchers = vouchersRepository.getVouchersByClient(client);
            return vouchers.stream()
                    .map(Voucher::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting client total voucher value for client {}: {}", client.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== TIME-BASED METRICS ==========

    private Long getClientTodayTransactions(Client client) {
        try {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.countByClientAndTxnDateBetween(client, todayDate, tomorrowDate);
        } catch (Exception e) {
            log.warn("Error getting client today transactions for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getClientTodaySpent(Client client) {
        try {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.findByClientAndTxnDateBetweenAndSettledTrue(client, todayDate, tomorrowDate);
            return transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting client today spent for client {}: {}", client.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getClientWeeklyTransactions(Client client) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.countByClientAndTxnDateAfter(client, oneWeekAgoDate);
        } catch (Exception e) {
            log.warn("Error getting client weekly transactions for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getClientWeeklySpent(Client client) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.findByClientAndTxnDateAfterAndSettledTrue(client, oneWeekAgoDate);
            return transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting client weekly spent for client {}: {}", client.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getClientMonthlyTransactions(Client client) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.countByClientAndTxnDateAfter(client, oneMonthAgoDate);
        } catch (Exception e) {
            log.warn("Error getting client monthly transactions for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getClientMonthlySpent(Client client) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.findByClientAndTxnDateAfterAndSettledTrue(client, oneMonthAgoDate);
            return transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting client monthly spent for client {}: {}", client.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== MERCHANT ANALYTICS ==========

    private Long getClientUniqueMerchants(Client client) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            return transactions.stream()
                    .map(Transaction::getMerchant)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        } catch (Exception e) {
            log.warn("Error getting client unique merchants for client {}: {}", client.getName(), e.getMessage());
            return 0L;
        }
    }

    private List<Map<String, Object>> getClientTopMerchants(Client client, Integer limit) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            Map<Merchant, List<Transaction>> merchantTransactions = new HashMap<>();

            // Group transactions by merchant
            for (Transaction transaction : transactions) {
                if (transaction.getMerchant() != null) {
                    merchantTransactions.computeIfAbsent(transaction.getMerchant(), k -> new ArrayList<>()).add(transaction);
                }
            }

            // Calculate merchant stats and sort by transaction count
            return merchantTransactions.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> merchant = new HashMap<>();
                        merchant.put("merchantId", entry.getKey().getId());
                        merchant.put("merchantName", entry.getKey().getName());
                        merchant.put("transactionCount", entry.getValue().size());
                        merchant.put("totalSpent", entry.getValue().stream()
                                .filter(Transaction::isSettled)
                                .map(Transaction::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                        return merchant;
                    })
                    .sorted((a, b) -> Integer.compare((Integer) b.get("transactionCount"), (Integer) a.get("transactionCount")))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting client top merchants for client {}: {}", client.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getClientMerchantUsageBreakdown(Client client) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            Map<Merchant, Long> merchantCounts = new HashMap<>();

            // Count transactions per merchant
            for (Transaction transaction : transactions) {
                if (transaction.getMerchant() != null) {
                    merchantCounts.merge(transaction.getMerchant(), 1L, Long::sum);
                }
            }

            long totalTransactions = transactions.size();
            if (totalTransactions == 0) return new ArrayList<>();

            return merchantCounts.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> merchant = new HashMap<>();
                        merchant.put("merchantName", entry.getKey().getName());
                        merchant.put("transactionCount", entry.getValue());
                        merchant.put("percentage", (double) entry.getValue() / totalTransactions * 100);
                        return merchant;
                    })
                    .sorted((a, b) -> Long.compare((Long) b.get("transactionCount"), (Long) a.get("transactionCount")))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting client merchant usage breakdown for client {}: {}", client.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== TREND ANALYTICS ==========

    private List<Map<String, Object>> getClientSpendingTrend(Client client, Integer days) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                Date startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

                List<Transaction> dayTransactions = transactionRepository.findByClientAndTxnDateBetweenAndSettledTrue(client, startOfDay, endOfDay);
                BigDecimal daySpent = dayTransactions.stream()
                        .map(Transaction::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.toString());
                dayData.put("spent", daySpent);
                dayData.put("transactions", dayTransactions.size());
                trend.add(dayData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting client spending trend for client {}: {}", client.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getClientTransactionTrend(Client client, Integer days) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                Date startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

                long dayTransactions = transactionRepository.countByClientAndTxnDateBetween(client, startOfDay, endOfDay);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.toString());
                dayData.put("transactions", dayTransactions);
                trend.add(dayData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting client transaction trend for client {}: {}", client.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getClientVoucherTrend(Client client, Integer months) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = months - 1; i >= 0; i--) {
                LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                Date startOfMonth = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfMonth = Date.from(monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());

                List<Voucher> vouchers = vouchersRepository.getVouchersByClient(client);
                long monthlyVouchers = vouchers.stream()
                        .filter(voucher -> voucher.getRequestDate() != null &&
                                voucher.getRequestDate().after(startOfMonth) &&
                                voucher.getRequestDate().before(endOfMonth))
                        .count();
                long approvedVouchers = vouchers.stream()
                        .filter(voucher -> voucher.getRequestDate() != null &&
                                voucher.getRequestDate().after(startOfMonth) &&
                                voucher.getRequestDate().before(endOfMonth) &&
                                voucher.isApproved())
                        .count();

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", monthStart.toString().substring(0, 7)); // YYYY-MM format
                monthData.put("totalVouchers", monthlyVouchers);
                monthData.put("approvedVouchers", approvedVouchers);
                trend.add(monthData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting client voucher trend for client {}: {}", client.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== PERFORMANCE METRICS ==========

    private String getClientLastTransactionDate(Client client) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            if (transactions.isEmpty()) return "No transactions";

            Transaction lastTransaction = transactions.stream()
                    .filter(t -> t.getTxnDate() != null)
                    .max(Comparator.comparing(Transaction::getTxnDate))
                    .orElse(null);

            if (lastTransaction != null) {
                return lastTransaction.getTxnDate().toString();
            }
            return "No transactions";
        } catch (Exception e) {
            log.warn("Error getting client last transaction date for client {}: {}", client.getName(), e.getMessage());
            return "Unknown";
        }
    }

    private Double getClientTransactionFrequency(Client client) {
        try {
            List<Transaction> transactions = transactionRepository.findByClient(client);
            if (transactions.size() < 2) return 0.0;

            // Calculate average days between transactions
            List<Date> sortedDates = transactions.stream()
                    .map(Transaction::getTxnDate)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            if (sortedDates.size() < 2) return 0.0;

            long totalDays = 0;
            for (int i = 1; i < sortedDates.size(); i++) {
                long daysBetween = (sortedDates.get(i).getTime() - sortedDates.get(i - 1).getTime()) / (1000 * 60 * 60 * 24);
                totalDays += daysBetween;
            }

            return (double) totalDays / (sortedDates.size() - 1);
        } catch (Exception e) {
            log.warn("Error calculating client transaction frequency for client {}: {}", client.getName(), e.getMessage());
            return 0.0;
        }
    }

    private Double getClientAverageDaysBetweenTransactions(Client client) {
        return getClientTransactionFrequency(client);
    }

    // ========== DEFAULT FALLBACK METHODS ==========

    private Map<String, Object> getDefaultClientAnalytics() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("totalTransactions", 0L);
        defaults.put("successfulTransactions", 0L);
        defaults.put("failedTransactions", 0L);
        defaults.put("successRate", 0.0);
        defaults.put("totalSpent", BigDecimal.ZERO);
        defaults.put("averageTransactionValue", BigDecimal.ZERO);
        defaults.put("totalVouchers", 0L);
        defaults.put("approvedVouchers", 0L);
        defaults.put("pendingVouchers", 0L);
        defaults.put("voucherApprovalRate", 0.0);
        defaults.put("totalVoucherValue", BigDecimal.ZERO);
        defaults.put("todayTransactions", 0L);
        defaults.put("todaySpent", BigDecimal.ZERO);
        defaults.put("weeklyTransactions", 0L);
        defaults.put("weeklySpent", BigDecimal.ZERO);
        defaults.put("monthlyTransactions", 0L);
        defaults.put("monthlySpent", BigDecimal.ZERO);
        defaults.put("uniqueMerchants", 0L);
        defaults.put("topMerchants", new ArrayList<>());
        defaults.put("merchantUsageBreakdown", new ArrayList<>());
        defaults.put("spendingTrend", new ArrayList<>());
        defaults.put("transactionTrend", new ArrayList<>());
        defaults.put("voucherTrend", new ArrayList<>());
        defaults.put("lastTransactionDate", "No transactions");
        defaults.put("transactionFrequency", 0.0);
        defaults.put("averageDaysBetweenTransactions", 0.0);
        return defaults;
    }
}
