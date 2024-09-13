package org.paykesho.organizationportal.vouchers.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.vouchers.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
