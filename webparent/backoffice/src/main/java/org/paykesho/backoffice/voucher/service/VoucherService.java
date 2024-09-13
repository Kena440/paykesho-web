package org.paykesho.backoffice.voucher.service;

import org.commons.entity.Voucher;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.voucher.repository.VouchersRepository;
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
    VouchersRepository vouchersRepository;

    @Autowired
    SettingService settingService;

    /**
     * +
     * This method lists all users in the system
     *
     * @return
     */
    public List<Voucher> listAll() {
        return (List<Voucher>) vouchersRepository.findAll(Sort.by("requestDate").descending());
    }

    /**
     * +
     * This method lists all users in the system by pagination
     *
     * @return
     */
    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, VOUCHERS_PER_PAGE, vouchersRepository);
    }

    public Long countAllVouchers() {
        return vouchersRepository.countAllVouchers();
    }

    public Voucher getVoucherById(Integer id) {
        return vouchersRepository.getVoucherById(id);
    }

    public Voucher save(Voucher voucher) throws PaykeshoError {
        if (voucher == null) {
            throw new PaykeshoError("Null parameter id");
        }
        return vouchersRepository.save(voucher);
    }

/*    public List<Voucher> getMerchantVouchers(int id) throws PaykeshoError {
        if (id == 0) {
            throw new PaykeshoError("Null parameter id");
        }

        return vouchersRepository.getVouchersByMerchantId(id);
    }*/
}
