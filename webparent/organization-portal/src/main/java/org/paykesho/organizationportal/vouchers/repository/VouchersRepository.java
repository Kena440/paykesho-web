package org.paykesho.organizationportal.vouchers.repository;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface VouchersRepository extends JpaRepository<Voucher, Integer> {

    @Query("select t from Voucher t where t.client.organization = :organization order by t.requestDate desc")
    public List<Voucher> getVouchersByOrganization(@Param("organization") Organization organization);

    @Query("select count(t.id) as total from Voucher t where t.client.organization = :organization")
    Long countAllVouchers(@Param("organization") Organization organization);

    @Query("select v from Voucher v where v.client = :client and v.approved = true and v.availableAmount > 0")
    public List<Voucher> getApprovedAndValidVouchers(@Param("client") Client client);

    long countByRequestDateAfter(Date date);
}
