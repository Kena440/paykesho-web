package org.paykesho.backoffice.voucher.repository;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface VouchersRepository extends SearchRepository<Voucher, Integer> {

    @Query("select v from Voucher v where v.client.id = :client order by v.requestDate desc")
    public List<Voucher> getVouchersByClient(@Param("client") Integer client);

    public Long countById(Integer id);

    @Query("select v from Voucher v where concat(v.client, ' ', v.amount, ' ', v.reference ) like %?1%")
    public Page<Voucher> findAll(String keyword, Pageable pageable);

    @Query("select count(v.id) as total from Voucher v order by v.requestDate desc")
    Long countAllVouchers();

    @Query("select v from Voucher v where v.id = ?1")
    Voucher getVoucherById(Integer id);

    @Query("update Voucher v set v.approved = ?2 where v.id = ?1")
    @Modifying
    void updateApprovedStatus(Integer id, boolean approved);

    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.client.id = :clientId AND v.repaymentDate IS NOT NULL AND v.repaymentDate > CURRENT_DATE")
    long countDelayedPayments(@Param("clientId") Integer clientId);

    // Custom query to get the outstanding balance for a client
    @Query("SELECT SUM(v.availableAmount) FROM Voucher v WHERE v.client.id = :clientId AND v.approved = true AND v.availableAmount > 0")
    BigDecimal getOutstandingBalance(@Param("clientId") Integer clientId);

    @Query("select v from Voucher v where v.client = :client and v.approved = true and v.availableAmount > 0")
    public List<Voucher> getApprovedAndValidVouchers(@Param("client") Client client);

    long countByRequestDateAfter(Date date);

}

