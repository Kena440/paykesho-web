package org.paykesho.clientportal.voucher.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.VoucherDto;
import org.paykesho.clientportal.voucher.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    public List<Voucher> getClientVouchers(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return voucherRepository.getVouchersByClient(client);
        }
    }

    public Voucher save(Voucher voucher) throws PaykeshoError {
        if (voucher == null) {
            throw new PaykeshoError("no voucher");
        } else {
            return voucherRepository.save(voucher);
        }
    }

    public List<Voucher> getApprovedAndValidClientVouchers(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return voucherRepository.getApprovedAndValidVouchers(client);
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

    public Voucher getVoucherById(Integer id) {
        return voucherRepository.getVoucherById(id);
    }

    public List<VoucherDto> getClientVouchersFlat(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return voucherRepository.getVouchersByClientFlat(client);
        }
    }

   /* public List<VoucherDto> getClientVouchersByMerchantFlat(Client client, MerchantDto merchant) throws PaykeshoError {
        if (client == null ) {
            throw new PaykeshoError("no client id");
        } else {
            return voucherRepository.getVouchersByClientAndMerchantFlat(client, merchant.getCode());
        }
    }*/

    public Voucher getVoucherByCode(String voucherCode) {
        return voucherRepository.getVoucherByReference(voucherCode);
    }
}
