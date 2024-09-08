package org.paykesho.organizationportal.repayments.repository;

import org.commons.entity.client.PayrollDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PayrollDeductionRepository extends JpaRepository<PayrollDeduction, Integer> {
    @Query("select count(c.id) from PayrollDeduction c")
    public Long countAllPayrollDeductions();

    List<PayrollDeduction> findByClientOrganizationId(Integer organizationId);

}