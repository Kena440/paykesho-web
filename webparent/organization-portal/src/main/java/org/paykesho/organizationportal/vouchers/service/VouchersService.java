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
        if (organization == null) {
            throw new PaykeshoError("no organization id");
        } else {
            return vouchersRepository.getVouchersByOrganization(organization);
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

        LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
        Date oneWeekAgoDate = Date.from(oneWeekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

        long totalVouchers = vouchersRepository.count();
        long vouchersLastWeek = vouchersRepository.countByRequestDateAfter(oneWeekAgoDate);

        double percentageChange = ((double) vouchersLastWeek / (totalVouchers - vouchersLastWeek)) * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVouchers", totalVouchers);
        stats.put("voucherPercentageChange", String.format("%.2f", percentageChange));

        return stats;
    }
}
