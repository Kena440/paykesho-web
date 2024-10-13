package org.paykesho.backoffice.voucher.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.voucher.repository.VouchersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class VoucherService {

    public static final int VOUCHERS_PER_PAGE = 10;

    @Autowired
    VouchersRepository vouchersRepository;
    @Autowired
    FileUploadService fileUploadService;

    /**
     * +
     * This method lists all users in the system
     *
     * @return
     */
    public List<Voucher> listAll() {
        List<Voucher> vouchers = (List<Voucher>) vouchersRepository.findAll(Sort.by("requestDate").descending());

        for (Voucher voucher : vouchers) {
            if (voucher.getPayslip() != null) {
                FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(voucher.getRemoteFileId());
                if (fileAccessResponse != null) {
                    voucher.setPayslipPath(fileAccessResponse.getFileUrl());
                }
            }
        }

        return vouchers;
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

    public List<Voucher> getApprovedAndValidClientVouchers(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return vouchersRepository.getApprovedAndValidVouchers(client);
        }
    }

/*    public List<Voucher> getMerchantVouchers(int id) throws PaykeshoError {
        if (id == 0) {
            throw new PaykeshoError("Null parameter id");
        }

        return vouchersRepository.getVouchersByMerchantId(id);
    }*/

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
