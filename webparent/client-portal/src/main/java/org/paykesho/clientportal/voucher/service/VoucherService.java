package org.paykesho.clientportal.voucher.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class VoucherService {

    @Autowired
    private VouchersRepository vouchersRepository;

    public List<Voucher> getClientVouchers(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return vouchersRepository.getVouchersByClient(client);
        }
    }

    public Voucher save(Voucher voucher) throws PaykeshoError {
        if (voucher == null) {
            throw new PaykeshoError("no voucher");
        } else {
            return vouchersRepository.save(voucher);
        }
    }

    public List<Voucher> getApprovedAndValidClientVouchers(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return vouchersRepository.getApprovedAndValidVouchers(client);
        }
    }

    public BigDecimal getApprovedAndValidClientVouchersBalance(Client client) throws PaykeshoError {
        List<Voucher> vouchers = getApprovedAndValidClientVouchers(client);
        BigDecimal totalBalance = BigDecimal.ZERO;

        for (Voucher voucher : vouchers) {
            totalBalance = totalBalance.add(voucher.getAmount());
        }

        return totalBalance;
    }

}
