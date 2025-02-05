package org.paykesho.clientportal.repayments.repository;

import org.commons.entity.client.Client;
import org.commons.entity.client.PayrollDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollDeductionRepository extends JpaRepository<PayrollDeduction, Integer> {

    List<PayrollDeduction> findByClient(Client client);

}