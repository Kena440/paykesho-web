package org.paykesho.organizationportal.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Transaction;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.paykesho.organizationportal.client.repository.ClientRepository;
import org.paykesho.organizationportal.transactions.repository.TransactionRepository;
import org.paykesho.organizationportal.vouchers.repository.VouchersRepository;
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
public class OrganizationDashboardAnalyticsService {

    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final VouchersRepository vouchersRepository;

    /**
     * Get comprehensive organization-specific analytics
     */
    public Map<String, Object> getOrganizationAnalytics(Organization organization) {
        try {
            log.debug("Generating analytics for organization: {}", organization != null ? organization.getName() : "null");

            if (organization == null) {
                log.warn("Organization is null, returning default analytics");
                return getDefaultOrganizationAnalytics();
            }

            Map<String, Object> analytics = new HashMap<>();

            // Basic metrics
            analytics.put("totalClients", getOrganizationClientCount(organization));
            analytics.put("activeClients", getOrganizationActiveClients(organization));
            analytics.put("inactiveClients", getOrganizationInactiveClients(organization));
            analytics.put("totalVouchers", getOrganizationVoucherCount(organization));
            analytics.put("approvedVouchers", getOrganizationApprovedVouchers(organization));
            analytics.put("pendingVouchers", getOrganizationPendingVouchers(organization));

            // Transaction metrics
            analytics.put("clientTransactionVolume", getOrganizationClientTransactionVolume(organization));
            analytics.put("clientTransactionCount", getOrganizationClientTransactionCount(organization));
            analytics.put("averageClientBalance", getOrganizationAverageClientBalance(organization));
            analytics.put("totalTransactionValue", getOrganizationTotalTransactionValue(organization));

            // Growth metrics
            analytics.put("newClientsThisMonth", getOrganizationNewClientsThisMonth(organization));
            analytics.put("newClientsThisWeek", getOrganizationNewClientsThisWeek(organization));
            analytics.put("clientGrowthRate", getOrganizationClientGrowthRate(organization));
            analytics.put("voucherApprovalRate", getOrganizationVoucherApprovalRate(organization));

            // Performance metrics
            analytics.put("todayTransactions", getOrganizationTodayTransactions(organization));
            analytics.put("todayRevenue", getOrganizationTodayRevenue(organization));
            analytics.put("weeklyTransactions", getOrganizationWeeklyTransactions(organization));
            analytics.put("weeklyRevenue", getOrganizationWeeklyRevenue(organization));
            analytics.put("monthlyTransactions", getOrganizationMonthlyTransactions(organization));
            analytics.put("monthlyRevenue", getOrganizationMonthlyRevenue(organization));

            // Top performers
            analytics.put("topClientsByActivity", getOrganizationTopClientsByActivity(organization, 10));
            analytics.put("topClientsByBalance", getOrganizationTopClientsByBalance(organization, 10));
            analytics.put("topClientsByVouchers", getOrganizationTopClientsByVouchers(organization, 10));

            // Trends
            analytics.put("clientGrowthTrend", getOrganizationClientGrowthTrend(organization, 12));
            analytics.put("voucherTrend", getOrganizationVoucherTrend(organization, 12));
            analytics.put("transactionTrend", getOrganizationTransactionTrend(organization, 30));

            log.debug("Successfully generated organization analytics for: {}", organization.getName());
            return analytics;

        } catch (Exception e) {
            log.error("Error generating organization analytics for organization {}: {}",
                    organization != null ? organization.getName() : "null", e.getMessage(), e);
            return getDefaultOrganizationAnalytics();
        }
    }

    // ========== BASIC METRICS ==========

    private Long getOrganizationClientCount(Organization organization) {
        try {
            if (organization == null) {
                log.warn("Organization is null for client count calculation");
                return 0L;
            }

            List<Client> clients = clientRepository.getOrganizationClients(organization);
            if (clients == null) {
                log.warn("Client repository returned null for organization: {}", organization.getName());
                return 0L;
            }

            return (long) clients.size();
        } catch (Exception e) {
            log.error("Error getting organization client count for organization {}: {}",
                    organization != null ? organization.getName() : "null", e.getMessage(), e);
            return 0L;
        }
    }

    private Long getOrganizationActiveClients(Organization organization) {
        try {
            List<Client> clients = clientRepository.getOrganizationClients(organization);
            return clients.stream()
                    .filter(Client::isEnabled)
                    .count();
        } catch (Exception e) {
            log.warn("Error getting organization active clients for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getOrganizationInactiveClients(Organization organization) {
        try {
            List<Client> clients = clientRepository.getOrganizationClients(organization);
            return clients.stream()
                    .filter(client -> !client.isEnabled())
                    .count();
        } catch (Exception e) {
            log.warn("Error getting organization inactive clients for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getOrganizationVoucherCount(Organization organization) {
        try {
            return vouchersRepository.countAllVouchers(organization);
        } catch (Exception e) {
            log.warn("Error getting organization voucher count for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getOrganizationApprovedVouchers(Organization organization) {
        try {
            List<Voucher> vouchers = vouchersRepository.getVouchersByOrganization(organization);
            return vouchers.stream()
                    .filter(Voucher::isApproved)
                    .count();
        } catch (Exception e) {
            log.warn("Error getting organization approved vouchers for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getOrganizationPendingVouchers(Organization organization) {
        try {
            List<Voucher> vouchers = vouchersRepository.getVouchersByOrganization(organization);
            return vouchers.stream()
                    .filter(voucher -> !voucher.isApproved())
                    .count();
        } catch (Exception e) {
            log.warn("Error getting organization pending vouchers for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    // ========== TRANSACTION METRICS ==========

    private BigDecimal getOrganizationClientTransactionVolume(Organization organization) {
        try {
            return transactionRepository.getTotalRevenueByOrganization(organization);
        } catch (Exception e) {
            log.warn("Error getting organization client transaction volume for organization {}: {}", organization.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getOrganizationClientTransactionCount(Organization organization) {
        try {
            return transactionRepository.countByOrganization(organization);
        } catch (Exception e) {
            log.warn("Error getting organization client transaction count for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getOrganizationAverageClientBalance(Organization organization) {
        try {
            List<Client> clients = clientRepository.getOrganizationClients(organization);
            if (clients.isEmpty()) return BigDecimal.ZERO;

            BigDecimal totalBalance = BigDecimal.ZERO;
            for (Client client : clients) {
                List<Transaction> clientTransactions = transactionRepository.findByClient(client);
                BigDecimal clientBalance = clientTransactions.stream()
                        .filter(Transaction::isSettled)
                        .map(Transaction::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalBalance = totalBalance.add(clientBalance);
            }

            return totalBalance.divide(new BigDecimal(clients.size()), 2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error getting organization average client balance for organization {}: {}", organization.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getOrganizationTotalTransactionValue(Organization organization) {
        return getOrganizationClientTransactionVolume(organization);
    }

    // ========== GROWTH METRICS ==========

    private Long getOrganizationNewClientsThisMonth(Organization organization) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Client> clients = clientRepository.getOrganizationClients(organization);
            return clients.stream()
                    .filter(client -> client.getCreatedTime() != null && client.getCreatedTime().after(oneMonthAgoDate))
                    .count();
        } catch (Exception e) {
            log.warn("Error getting organization new clients this month for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private Long getOrganizationNewClientsThisWeek(Organization organization) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Client> clients = clientRepository.getOrganizationClients(organization);
            return clients.stream()
                    .filter(client -> client.getCreatedTime() != null && client.getCreatedTime().after(oneWeekAgoDate))
                    .count();
        } catch (Exception e) {
            log.warn("Error getting organization new clients this week for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private Double getOrganizationClientGrowthRate(Organization organization) {
        try {
            long thisMonth = getOrganizationNewClientsThisMonth(organization);
            LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
            Date twoMonthsAgoDate = Date.from(twoMonthsAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Client> clients = clientRepository.getOrganizationClients(organization);
            long lastMonth = clients.stream()
                    .filter(client -> client.getCreatedTime() != null &&
                            client.getCreatedTime().after(twoMonthsAgoDate) &&
                            client.getCreatedTime().before(oneMonthAgoDate))
                    .count();

            if (lastMonth == 0) return 0.0;
            return ((double) (thisMonth - lastMonth) / lastMonth) * 100;
        } catch (Exception e) {
            log.warn("Error calculating organization client growth rate for organization {}: {}", organization.getName(), e.getMessage());
            return 0.0;
        }
    }

    private Double getOrganizationVoucherApprovalRate(Organization organization) {
        try {
            long totalVouchers = getOrganizationVoucherCount(organization);
            long approvedVouchers = getOrganizationApprovedVouchers(organization);
            if (totalVouchers == 0) return 0.0;
            return (double) approvedVouchers / totalVouchers * 100;
        } catch (Exception e) {
            log.warn("Error calculating organization voucher approval rate for organization {}: {}", organization.getName(), e.getMessage());
            return 0.0;
        }
    }

    // ========== PERFORMANCE METRICS ==========

    private Long getOrganizationTodayTransactions(Organization organization) {
        try {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.countByOrganizationAndTxnDateBetween(organization, todayDate, tomorrowDate);
        } catch (Exception e) {
            log.warn("Error getting organization today transactions for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getOrganizationTodayRevenue(Organization organization) {
        try {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date tomorrowDate = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.getRevenueByOrganizationAndDateBetween(organization, todayDate, tomorrowDate);
        } catch (Exception e) {
            log.warn("Error getting organization today revenue for organization {}: {}", organization.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getOrganizationWeeklyTransactions(Organization organization) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.countByOrganizationAndTxnDateAfter(organization, oneWeekAgoDate);
        } catch (Exception e) {
            log.warn("Error getting organization weekly transactions for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getOrganizationWeeklyRevenue(Organization organization) {
        try {
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.getRevenueByOrganizationAndDateAfter(organization, oneWeekAgoDate);
        } catch (Exception e) {
            log.warn("Error getting organization weekly revenue for organization {}: {}", organization.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getOrganizationMonthlyTransactions(Organization organization) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.countByOrganizationAndTxnDateAfter(organization, oneMonthAgoDate);
        } catch (Exception e) {
            log.warn("Error getting organization monthly transactions for organization {}: {}", organization.getName(), e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getOrganizationMonthlyRevenue(Organization organization) {
        try {
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            return transactionRepository.getRevenueByOrganizationAndDateAfter(organization, oneMonthAgoDate);
        } catch (Exception e) {
            log.warn("Error getting organization monthly revenue for organization {}: {}", organization.getName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ========== TOP PERFORMERS ==========

    private List<Map<String, Object>> getOrganizationTopClientsByActivity(Organization organization, Integer limit) {
        try {
            List<Object[]> results = transactionRepository.findTopClientsByTransactionCount(organization);
            List<Map<String, Object>> clientActivity = new ArrayList<>();

            for (Object[] result : results) {
                Map<String, Object> clientData = new HashMap<>();
                clientData.put("clientId", result[0]);
                clientData.put("clientName", result[1]);
                clientData.put("transactionCount", result[2]);
                clientActivity.add(clientData);
            }

            // Apply limit in service layer
            return clientActivity.stream()
                    .limit(limit != null ? limit : 10)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting organization top clients by activity for organization {}: {}", organization.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getOrganizationTopClientsByBalance(Organization organization, Integer limit) {
        try {
            List<Object[]> results = transactionRepository.findTopClientsByTransactionValue(organization);
            List<Map<String, Object>> clientBalances = new ArrayList<>();

            for (Object[] result : results) {
                Map<String, Object> clientData = new HashMap<>();
                clientData.put("clientId", result[0]);
                clientData.put("clientName", result[1]);
                clientData.put("balance", result[2]);
                clientBalances.add(clientData);
            }

            // Apply limit in service layer
            return clientBalances.stream()
                    .limit(limit != null ? limit : 10)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting organization top clients by balance for organization {}: {}", organization.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getOrganizationTopClientsByVouchers(Organization organization, Integer limit) {
        try {
            List<Client> clients = clientRepository.getOrganizationClients(organization);
            List<Map<String, Object>> clientVouchers = new ArrayList<>();

            for (Client client : clients) {
                List<Voucher> clientVoucherList = vouchersRepository.getApprovedAndValidVouchers(client);
                Map<String, Object> clientData = new HashMap<>();
                clientData.put("clientId", client.getId());
                clientData.put("clientName", client.getName());
                clientData.put("voucherCount", clientVoucherList.size());
                clientData.put("approvedVouchers", clientVoucherList.stream()
                        .filter(Voucher::isApproved)
                        .count());
                clientData.put("totalVoucherValue", clientVoucherList.stream()
                        .map(Voucher::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                clientVouchers.add(clientData);
            }

            return clientVouchers.stream()
                    .sorted((a, b) -> Integer.compare((Integer) b.get("voucherCount"), (Integer) a.get("voucherCount")))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting organization top clients by vouchers for organization {}: {}", organization.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== TREND ANALYTICS ==========

    private List<Map<String, Object>> getOrganizationClientGrowthTrend(Organization organization, Integer months) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = months - 1; i >= 0; i--) {
                LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                Date startOfMonth = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfMonth = Date.from(monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());

                List<Client> clients = clientRepository.getOrganizationClients(organization);
                long newClients = clients.stream()
                        .filter(client -> client.getCreatedTime() != null &&
                                client.getCreatedTime().after(startOfMonth) &&
                                client.getCreatedTime().before(endOfMonth))
                        .count();

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", monthStart.toString().substring(0, 7)); // YYYY-MM format
                monthData.put("newClients", newClients);
                trend.add(monthData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting organization client growth trend for organization {}: {}", organization.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getOrganizationVoucherTrend(Organization organization, Integer months) {
        try {
            List<Map<String, Object>> trend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();

            for (int i = months - 1; i >= 0; i--) {
                LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                Date startOfMonth = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endOfMonth = Date.from(monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());

                List<Voucher> allVouchers = vouchersRepository.getVouchersByOrganization(organization);
                long monthlyVouchers = allVouchers.stream()
                        .filter(voucher -> voucher.getRequestDate() != null &&
                                voucher.getRequestDate().after(startOfMonth) &&
                                voucher.getRequestDate().before(endOfMonth))
                        .count();
                long approvedVouchers = allVouchers.stream()
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
            log.warn("Error getting organization voucher trend for organization {}: {}", organization.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getOrganizationTransactionTrend(Organization organization, Integer days) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);
            Date startDateObj = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDateObj = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Object[]> results = transactionRepository.getDailyTransactionTrend(organization, startDateObj, endDateObj);
            List<Map<String, Object>> trend = new ArrayList<>();

            for (Object[] result : results) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", result[0].toString());
                dayData.put("transactions", result[1]);
                dayData.put("revenue", result[2]);
                trend.add(dayData);
            }

            return trend;
        } catch (Exception e) {
            log.warn("Error getting organization transaction trend for organization {}: {}", organization.getName(), e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== DEFAULT FALLBACK METHODS ==========

    private Map<String, Object> getDefaultOrganizationAnalytics() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("totalClients", 0L);
        defaults.put("activeClients", 0L);
        defaults.put("inactiveClients", 0L);
        defaults.put("totalVouchers", 0L);
        defaults.put("approvedVouchers", 0L);
        defaults.put("pendingVouchers", 0L);
        defaults.put("clientTransactionVolume", BigDecimal.ZERO);
        defaults.put("clientTransactionCount", 0L);
        defaults.put("averageClientBalance", BigDecimal.ZERO);
        defaults.put("totalTransactionValue", BigDecimal.ZERO);
        defaults.put("newClientsThisMonth", 0L);
        defaults.put("newClientsThisWeek", 0L);
        defaults.put("clientGrowthRate", 0.0);
        defaults.put("voucherApprovalRate", 0.0);
        defaults.put("todayTransactions", 0L);
        defaults.put("todayRevenue", BigDecimal.ZERO);
        defaults.put("weeklyTransactions", 0L);
        defaults.put("weeklyRevenue", BigDecimal.ZERO);
        defaults.put("monthlyTransactions", 0L);
        defaults.put("monthlyRevenue", BigDecimal.ZERO);
        defaults.put("topClientsByActivity", new ArrayList<>());
        defaults.put("topClientsByBalance", new ArrayList<>());
        defaults.put("topClientsByVouchers", new ArrayList<>());
        defaults.put("clientGrowthTrend", new ArrayList<>());
        defaults.put("voucherTrend", new ArrayList<>());
        defaults.put("transactionTrend", new ArrayList<>());
        return defaults;
    }
}
