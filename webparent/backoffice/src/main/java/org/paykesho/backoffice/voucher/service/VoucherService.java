package org.paykesho.backoffice.voucher.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
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

@Slf4j
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
        try {
            log.debug("Retrieving all vouchers");
            List<Voucher> vouchers = (List<Voucher>) vouchersRepository.findAll(Sort.by("requestDate").descending());
            log.debug("Found {} vouchers, enriching with file access details", vouchers.size());

            for (Voucher voucher : vouchers) {
                if (voucher.getPayslip() != null) {
                    try {
                        FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(voucher.getRemoteFileId());
                        if (fileAccessResponse != null) {
                            voucher.setPayslipPath(fileAccessResponse.getFileUrl());
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to get file access details for voucher ID {}: {}", voucher.getId(), ex.getMessage());
                        voucher.setPayslipPath(null);
                    }
                }
            }

            log.debug("Successfully retrieved and enriched {} vouchers", vouchers.size());
            return vouchers;
        } catch (Exception ex) {
            log.error("Error retrieving all vouchers: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve all vouchers", ex);
        }
    }

    /**
     * +
     * This method lists all users in the system by pagination
     *
     * @return
     */
    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        try {
            log.debug("Listing vouchers by page: {}", pageNum);
            helper.listEntities(pageNum, VOUCHERS_PER_PAGE, vouchersRepository);
            log.debug("Successfully listed vouchers for page: {}", pageNum);
        } catch (PaykeshoError ex) {
            log.warn("PaykeshoError listing vouchers by page {}: {}", pageNum, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error listing vouchers by page {}: {}", pageNum, ex.getMessage(), ex);
            throw new RuntimeException("Failed to list vouchers by page", ex);
        }
    }

    public Long countAllVouchers() {
        try {
            log.debug("Counting all vouchers");
            Long count = vouchersRepository.countAllVouchers();
            log.debug("Total voucher count: {}", count);
            return count;
        } catch (Exception ex) {
            log.error("Error counting all vouchers: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to count all vouchers", ex);
        }
    }

    public Voucher getVoucherById(Integer id) {
        try {
            log.debug("Retrieving voucher by ID: {}", id);
            Voucher voucher = vouchersRepository.getVoucherById(id);
            if (voucher != null) {
                log.debug("Found voucher with ID: {}", id);
            } else {
                log.debug("No voucher found with ID: {}", id);
            }
            return voucher;
        } catch (Exception ex) {
            log.error("Error retrieving voucher by ID {}: {}", id, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve voucher by ID", ex);
        }
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
