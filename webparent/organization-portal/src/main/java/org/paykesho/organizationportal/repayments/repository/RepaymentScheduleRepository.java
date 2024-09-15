package org.paykesho.organizationportal.repayments.repository;

import org.commons.entity.client.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Integer> {
    List<RepaymentSchedule> findByClientId(Long clientId);

    List<RepaymentSchedule> findByClientOrganizationId(Integer organizationId);

    RepaymentSchedule findByReference(String reference);

    RepaymentSchedule findDistinctFirstByClientIdOrderByIdDesc(Integer clientId);

    @Query("select count(c.id) from RepaymentSchedule c")
    public Long countAllRepaymentSchedules();
}