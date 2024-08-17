package org.paykesho.clientportal.voucher.repository;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.paykesho.clientportal.api.dto.VoucherDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    @Query("select v from Voucher v where v.client = :client")
    public List<Voucher> getVouchersByClient(@Param("client") Client client);

    @Query("select new org.paykesho.clientportal.api.dto.VoucherDto(v.reference, v.amount, v.availableAmount, v.repaymentAmount, v.interestPercent, v.repaymentCycle, v.repaymentDate, v.requestDate, v.approved) from Voucher v where v.client = :client")
    public List<VoucherDto> getVouchersByClientFlat(@Param("client") Client client);

    @Query("select v from Voucher v where v.client = :client and v.approved = true and v.availableAmount > 0")
    public List<Voucher> getApprovedAndValidVouchers(@Param("client") Client client);

    @Query("select count(v.id) as total from Voucher v")
    Long countAllVouchers();

    @Query("select v from Voucher v where v.id = ?1")
    Voucher getVoucherById(Integer id);

    /*  @Query("select new org.paykesho.clientportal.api.dto.VoucherDto(v.reference, v.amount, v.availableAmount, v.repaymentAmount, v.interestPercent, v.repaymentCycle, v.repaymentDate, v.requestDate, v.approved) from Voucher v where v.client = :client and v.merchant.paykeshoId = :merchantCode")
      List<VoucherDto> getVouchersByClientAndMerchantFlat(Client client, String merchantCode);
  */
    Voucher getVoucherByReference(String voucherCode);
}

