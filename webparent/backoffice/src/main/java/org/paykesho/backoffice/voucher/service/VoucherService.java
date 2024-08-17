package org.paykesho.backoffice.voucher.service;

import org.commons.entity.Voucher;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.voucher.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class VoucherService {

    public static final int VOUCHERS_PER_PAGE = 10;

    @Autowired
    VoucherRepository voucherRepository;

    @Autowired
    SettingService settingService;

    /**
     * +
     * This method lists all users in the system
     *
     * @return
     */
    public List<Voucher> listAll() {
        return (List<Voucher>) voucherRepository.findAll(Sort.by("voucherRequestDate").descending());
    }

    /**
     * +
     * This method lists all users in the system by pagination
     *
     * @return
     */
    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, VOUCHERS_PER_PAGE, voucherRepository);
    }

    public Long countAllVouchers() {
        return voucherRepository.countAllVouchers();
    }

    public Voucher getVoucherById(Integer id) {
        return voucherRepository.getVoucherById(id);
    }

    public Voucher save(Voucher voucher) throws PaykeshoError {
        if (voucher == null) {
            throw new PaykeshoError("Null parameter id");
        }
        return voucherRepository.save(voucher);
    }

/*    public List<Voucher> getMerchantVouchers(int id) throws PaykeshoError {
        if (id == 0) {
            throw new PaykeshoError("Null parameter id");
        }

        return voucherRepository.getVouchersByMerchantId(id);
    }*/
}
