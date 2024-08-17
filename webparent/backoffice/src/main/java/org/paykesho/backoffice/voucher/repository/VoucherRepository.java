package org.paykesho.backoffice.voucher.repository;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoucherRepository extends SearchRepository<Voucher, Integer> {

    @Query("select v from Voucher v where v.client.id = :client")
    public Client getVoucherByClient(@Param("client") Integer client);

    public Long countById(Integer id);

    @Query("select v from Voucher v where concat(v.client, ' ', v.merchant.name, ' ', v.reference ) like %?1%")
    public Page<Voucher> findAll(String keyword, Pageable pageable);

    @Query("select count(v.id) as total from Voucher v")
    Long countAllVouchers();

    @Query("select v from Voucher v where v.id = ?1")
    Voucher getVoucherById(Integer id);

    @Query("update Voucher v set v.approved = ?2 where v.id = ?1")
    @Modifying
    void updateApprovedStatus(Integer id, boolean approved);

    @Query("select v from Voucher v where v.merchant.id = ?1")
    List<Voucher> getVouchersByMerchantId(int id);
}

