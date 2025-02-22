package org.paykesho.clientportal.voucher.repository;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface VouchersRepository extends JpaRepository<Voucher, Integer> {

    @Query("select v from Voucher v where v.client = :client order by v.requestDate desc")
    public List<Voucher> getVouchersByClient(@Param("client") Client client);

    @Query("select v from Voucher v where v.client = :client and v.approved = true and v.availableAmount > 0")
    public List<Voucher> getApprovedAndValidVouchers(@Param("client") Client client);

    @Query("select count(v.id) as total from Voucher v")
    Long countAllVouchers();

    @Query("select v from Voucher v where v.id = ?1")
    Voucher getVoucherById(Integer id);

    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.client.id = :clientId AND v.firstRepaymentDate IS NOT NULL AND v.firstRepaymentDate > CURRENT_DATE")
    long countDelayedPayments(@Param("clientId") Integer clientId);

    // Custom query to get the outstanding balance for a client
    @Query("SELECT SUM(v.availableAmount) FROM Voucher v WHERE v.client.id = :clientId AND v.approved = true AND v.availableAmount > 0")
    BigDecimal getOutstandingBalance(@Param("clientId") Integer clientId);
}

