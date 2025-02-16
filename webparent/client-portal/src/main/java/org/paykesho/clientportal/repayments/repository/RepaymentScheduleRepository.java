package org.paykesho.clientportal.repayments.repository;

import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Integer> {
    List<RepaymentSchedule> findByClientId(Integer clientId);

    RepaymentSchedule findByReference(String reference);

    RepaymentSchedule findDistinctFirstByClientIdOrderByIdDesc(Integer clientId);

    List<RepaymentSchedule> findByClientIdAndStatus(Integer clientId, PaymentStatus status);

}