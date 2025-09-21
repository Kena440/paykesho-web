package org.paykesho.organizationportal.vouchers.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.vouchers.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VouchersService {

    @Autowired
    private VouchersRepository vouchersRepository;

    public List<Voucher> getOrganizationVouchers(Organization organization) throws PaykeshoError {
        try {
            log.debug("Retrieving vouchers for organization ID: {}", organization != null ? organization.getId() : "null");
            if (organization == null) {
                throw new PaykeshoError("no organization id");
            } else {
                List<Voucher> vouchers = vouchersRepository.getVouchersByOrganization(organization);
                log.debug("Successfully retrieved {} vouchers for organization ID: {}", vouchers.size(), organization.getId());
                return vouchers;
            }
        } catch (PaykeshoError ex) {
            log.warn("PaykeshoError retrieving organization vouchers: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error retrieving vouchers for organization ID {}: {}", organization != null ? organization.getId() : "null", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve organization vouchers", ex);
        }
    }

    public List<Voucher> getApprovedAndValidClientVouchers(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return vouchersRepository.getApprovedAndValidVouchers(client);
        }
    }

    public Map<String, Object> getVoucherStats() {
        try {
            log.debug("Calculating voucher statistics");
            LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
            Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

            long totalVouchers = vouchersRepository.count();
            long vouchersLastWeek = vouchersRepository.countByRequestDateAfter(oneWeekAgoDate);

            double percentageChange = 0.0;
            if (totalVouchers > vouchersLastWeek) {
                percentageChange = ((double) vouchersLastWeek / (totalVouchers - vouchersLastWeek)) * 100;
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalVouchers", totalVouchers);
            stats.put("voucherPercentageChange", String.format("%.2f", percentageChange));

            log.debug("Voucher stats - Total: {}, Last week: {}, Percentage change: {}",
                    totalVouchers, vouchersLastWeek, percentageChange);
            return stats;
        } catch (Exception ex) {
            log.error("Error calculating voucher statistics: {}", ex.getMessage(), ex);
            // Return default stats on error
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("totalVouchers", 0L);
            defaultStats.put("voucherPercentageChange", "0.00");
            return defaultStats;
        }
    }
}
