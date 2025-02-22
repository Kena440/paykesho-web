package org.paykesho.organizationportal.repayments.repository;

import org.commons.entity.client.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Integer> {
    List<RepaymentSchedule> findByClientId(Long clientId);

    List<RepaymentSchedule> findByClientOrganizationId(Integer organizationId);

    RepaymentSchedule findByReference(String reference);

    RepaymentSchedule findDistinctFirstByClientIdOrderByIdDesc(Integer clientId);

    @Query("select count(c.id) from RepaymentSchedule c")
    public Long countAllRepaymentSchedules();

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate BETWEEN :startDate AND :endDate AND rs.status = 'PENDING' AND rs.client.organization.id = :organizationId")
    List<RepaymentSchedule> findByDueDateBetween(
            @Param("organizationId") Integer organizationId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

}