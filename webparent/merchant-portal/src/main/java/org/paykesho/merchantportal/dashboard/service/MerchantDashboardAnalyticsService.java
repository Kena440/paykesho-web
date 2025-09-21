package org.paykesho.merchantportal.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.merchant.Merchant;
import org.paykesho.merchantportal.transactions.repository.TransactionRepository;
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
public class MerchantDashboardAnalyticsService {

    private final TransactionRepository transactionRepository;

    /**
     * Get comprehensive merchant-specific analytics
     */
    public Map<String, Object> getMerchantAnalytics(Merchant merchant) {
        try {
            log.debug("Generating analytics for merchant: {}", merchant != null ? merchant.getName() : "null");

            if (merchant == null) {
                log.warn("Merchant is null, returning default analytics");
                return getDefaultMerchantAnalytics();
            }

            Map<String, Object> analytics = new HashMap<>();

            // Basic metrics
            analytics.put("totalTransactions", getMerchantTransactionCount(merchant));
            analytics.put("totalTransactionValue", getMerchantTransactionValue(merchant));
            analytics.put("successfulTransactions", getMerchantSuccessfulTransactions(merchant));
            analytics.put("failedTransactions", getMerchantFailedTransactions(merchant));
            analytics.put("successRate", getMerchantSuccessRate(merchant));

            // Time-based metrics
            analytics.put("dailyTransactions", getMerchantDailyTransactions(merchant));
            analytics.put("weeklyTransactions", getMerchantWeeklyTransactions(merchant));
            analytics.put("monthlyTransactions", getMerchantMonthlyTransactions(merchant));
            analytics.put("dailyRevenue", getMerchantDailyRevenue(merchant));
            analytics.put("weeklyRevenue", getMerchantWeeklyRevenue(merchant));
            analytics.put("monthlyRevenue", getMerchantMonthlyRevenue(merchant));

            // Customer analytics
            analytics.put("uniqueCustomers", getMerchantUniqueCustomers(merchant));
            analytics.put("averageTransactionValue", getMerchantAverageTransactionValue(merchant));
            analytics.put("topCustomers", getMerchantTopCustomers(merchant, 5));

            // Trends
            analytics.put("revenueTrend", getMerchantRevenueTrend(merchant, 30));
            analytics.put("transactionTrend", getMerchantTransactionTrend(merchant, 30));

            // Performance metrics
            analytics.put("todayTransactions", getMerchantTodayTransactions(merchant));
            analytics.put("todayRevenue", getMerchantTodayRevenue(merchant));
            analytics.put("yesterdayTransactions", getMerchantYesterdayTransactions(merchant));
            analytics.put("yesterdayRevenue", getMerchantYesterdayRevenue(merchant));

            // Growth metrics
            analytics.put("weeklyGrowth", getMerchantWeeklyGrowth(merchant));
            analytics.put("monthlyGrowth", getMerchantMonthlyGrowth(merchant));

            log.debug("Successfully generated merchant analytics for: {}", merchant.getName());
            return analytics;

        } catch (Exception e) {
            log.error("Error generating merchant analytics for merchant {}: {}",
                    merchant != null ? merchant.getName() : "null", e.getMessage(), e);
            return getDefaultMerchantAnalytics();
        }
    }

    // ========== BASIC METRICS ==========

    private Long getMerchantTransactionCount(Merchant merchant) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return (long) transactions.size();
        } catch (Exception e) {
            log.warn("Error getting merchant transaction count for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getMerchantTransactionValue(Merchant merchant) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting merchant transaction value for merchant {}: {}", merchant.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getMerchantSuccessfulTransactions(Merchant merchant) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.isSettled())
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant successful transactions for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getMerchantFailedTransactions(Merchant merchant) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> !t.isSettled())
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant failed transactions for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private Double getMerchantSuccessRate(Merchant merchant) {
        try {
            long total = getMerchantTransactionCount(merchant);
            long successful = getMerchantSuccessfulTransactions(merchant);
            if (total == 0) return 0.0;
            return (double) successful / total * 100;
        } catch (Exception e) {
            log.warn("Error calculating merchant success rate for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0.0;
        }
    }

    // ========== TIME-BASED METRICS ==========

    private Long getMerchantDailyTransactions(Merchant merchant) {
        try {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(todayDate) &&
                            t.getTxnDate().before(tomorrowDate))
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant daily transactions for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getMerchantWeeklyTransactions(Merchant merchant) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().after(oneWeekAgoDate))
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant weekly transactions for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getMerchantMonthlyTransactions(Merchant merchant) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().after(oneMonthAgoDate))
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant monthly transactions for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getMerchantDailyRevenue(Merchant merchant) {
        try {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(todayDate) &&
                            t.getTxnDate().before(tomorrowDate) &&
                            t.isSettled())
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting merchant daily revenue for merchant {}: {}", merchant.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getMerchantWeeklyRevenue(Merchant merchant) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(oneWeekAgoDate) &&
                            t.isSettled())
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting merchant weekly revenue for merchant {}: {}", merchant.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getMerchantMonthlyRevenue(Merchant merchant) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(oneMonthAgoDate) &&
                            t.isSettled())
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting merchant monthly revenue for merchant {}: {}", merchant.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== CUSTOMER ANALYTICS ==========

    private Long getMerchantUniqueCustomers(Merchant merchant) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .map(Transaction::getClient)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant unique customers for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getMerchantAverageTransactionValue(Merchant merchant) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            if (transactions.isEmpty()) return BigDecimal.ZERO;

            BigDecimal totalValue = transactions.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return totalValue.divide(new BigDecimal(transactions.size()), 2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error getting merchant average transaction value for merchant {}: {}", merchant.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> getMerchantTopCustomers(Merchant merchant, Integer limit) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            Map<Object, List<Transaction>> customerTransactions = new HashMap<>();

            // Group transactions by client
            for (Transaction transaction : transactions) {
                if (transaction.getClient() != null) {
                    customerTransactions.computeIfAbsent(transaction.getClient(), k -> new ArrayList<>()).add(transaction);
                }
            }

            // Calculate customer stats and sort by transaction count
            return customerTransactions.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> customer = new HashMap<>();
                        customer.put("clientId", entry.getKey().toString());
                        customer.put("transactionCount", entry.getValue().size());
                        customer.put("totalValue", entry.getValue().stream()
                                .map(Transaction::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                        return customer;
                    })
                    .sorted((a, b) -> Integer.compare((Integer) b.get("transactionCount"), (Integer) a.get("transactionCount")))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting merchant top customers for merchant {}: {}", merchant.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== TREND ANALYTICS ==========

    private List<Map<String, Object>> getMerchantRevenueTrend(Merchant merchant, Integer days) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                Date startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

                List<Transaction> dayTransactions = transactionRepository.getTransactionsByMerchant(merchant).stream()
                        .filter(t -> t.getTxnDate() != null &&
                                t.getTxnDate().after(startOfDay) &&
                                t.getTxnDate().before(endOfDay) &&
                                t.isSettled())
                        .collect(java.util.stream.Collectors.toList());

                BigDecimal dayRevenue = dayTransactions.stream()
                        .map(Transaction::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.toString());
                dayData.put("revenue", dayRevenue);
                dayData.put("transactions", dayTransactions.size());
                trend.add(dayData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting merchant revenue trend for merchant {}: {}", merchant.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getMerchantTransactionTrend(Merchant merchant, Integer days) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                Date startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

                long dayTransactions = transactionRepository.getTransactionsByMerchant(merchant).stream()
                        .filter(t -> t.getTxnDate() != null &&
                                t.getTxnDate().after(startOfDay) &&
                                t.getTxnDate().before(endOfDay))
                        .count();

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.toString());
                dayData.put("transactions", dayTransactions);
                trend.add(dayData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting merchant transaction trend for merchant {}: {}", merchant.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== PERFORMANCE METRICS ==========

    private Long getMerchantTodayTransactions(Merchant merchant) {
        return getMerchantDailyTransactions(merchant);
    }

    private BigDecimal getMerchantTodayRevenue(Merchant merchant) {
        return getMerchantDailyRevenue(merchant);
    }

    private Long getMerchantYesterdayTransactions(Merchant merchant) {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Date yesterdayDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date todayDate = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(yesterdayDate) &&
                            t.getTxnDate().before(todayDate))
                    .count();
        } catch (Exception e) {
            log.warn("Error getting merchant yesterday transactions for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getMerchantYesterdayRevenue(Merchant merchant) {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Date yesterdayDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date todayDate = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> transactions = transactionRepository.getTransactionsByMerchant(merchant);
            return transactions.stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(yesterdayDate) &&
                            t.getTxnDate().before(todayDate) &&
                            t.isSettled())
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error getting merchant yesterday revenue for merchant {}: {}", merchant.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== GROWTH METRICS ==========

    private Double getMerchantWeeklyGrowth(Merchant merchant) {
        try {
            long thisWeek = getMerchantWeeklyTransactions(merchant);
            LocalDate twoWeeksAgo = LocalDate.now().minusWeeks(2);
            Date twoWeeksAgoDate = Date.from(twoWeeksAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> lastWeekTransactions = transactionRepository.getTransactionsByMerchant(merchant).stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(twoWeeksAgoDate) &&
                            t.getTxnDate().before(oneWeekAgoDate))
                    .collect(java.util.stream.Collectors.toList());

            long lastWeek = lastWeekTransactions.size();
            if (lastWeek == 0) return 0.0;

            return ((double) (thisWeek - lastWeek) / lastWeek) * 100;
        } catch (Exception e) {
            log.warn("Error calculating merchant weekly growth for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0.0;
        }
    }

    private Double getMerchantMonthlyGrowth(Merchant merchant) {
        try {
            long thisMonth = getMerchantMonthlyTransactions(merchant);
            LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
            Date twoMonthsAgoDate = Date.from(twoMonthsAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Transaction> lastMonthTransactions = transactionRepository.getTransactionsByMerchant(merchant).stream()
                    .filter(t -> t.getTxnDate() != null &&
                            t.getTxnDate().after(twoMonthsAgoDate) &&
                            t.getTxnDate().before(oneMonthAgoDate))
                    .collect(java.util.stream.Collectors.toList());

            long lastMonth = lastMonthTransactions.size();
            if (lastMonth == 0) return 0.0;

            return ((double) (thisMonth - lastMonth) / lastMonth) * 100;
        } catch (Exception e) {
            log.warn("Error calculating merchant monthly growth for merchant {}: {}", merchant.getName(), e.getMessage());
            return 0.0;
        }
    }

    // ========== DEFAULT FALLBACK METHODS ==========

    private Map<String, Object> getDefaultMerchantAnalytics() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("totalTransactions", 0L);
        defaults.put("totalTransactionValue", BigDecimal.ZERO);
        defaults.put("successfulTransactions", 0L);
        defaults.put("failedTransactions", 0L);
        defaults.put("successRate", 0.0);
        defaults.put("dailyTransactions", 0L);
        defaults.put("weeklyTransactions", 0L);
        defaults.put("monthlyTransactions", 0L);
        defaults.put("dailyRevenue", BigDecimal.ZERO);
        defaults.put("weeklyRevenue", BigDecimal.ZERO);
        defaults.put("monthlyRevenue", BigDecimal.ZERO);
        defaults.put("uniqueCustomers", 0L);
        defaults.put("averageTransactionValue", BigDecimal.ZERO);
        defaults.put("topCustomers", new ArrayList<>());
        defaults.put("revenueTrend", new ArrayList<>());
        defaults.put("transactionTrend", new ArrayList<>());
        defaults.put("todayTransactions", 0L);
        defaults.put("todayRevenue", BigDecimal.ZERO);
        defaults.put("yesterdayTransactions", 0L);
        defaults.put("yesterdayRevenue", BigDecimal.ZERO);
        defaults.put("weeklyGrowth", 0.0);
        defaults.put("monthlyGrowth", 0.0);
        return defaults;
    }
}
