package org.paykesho.clientportal.repayments.repository;

import org.commons.entity.client.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Integer> {
    List<RepaymentSchedule> findByClientId(Integer clientId);

    RepaymentSchedule findByReference(String reference);

    RepaymentSchedule findDistinctFirstByClientIdOrderByIdDesc(Integer clientId);
}