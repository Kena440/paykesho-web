package org.paykesho.backoffice.repayments.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.PaymentStatus;
import org.paykesho.backoffice.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.backoffice.repayments.repository.PayrollDeductionRepository;
import org.paykesho.backoffice.repayments.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class RepaymentService {

    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;


    public List<RepaymentSchedule> getRepaymentSchedules() {
        return repaymentScheduleRepository.findAll();
    }

    public List<PayrollDeduction> getPayrollDeductions() {
        return payrollDeductionRepository.findAll();
    }

    public void createRepaymentSchedule(RepaymentScheduleRequest request) {
        RepaymentSchedule schedule = new RepaymentSchedule();

        // Set values from request
        schedule.setReference(request.getReference());
        schedule.setClient(request.getClient());
        schedule.setVoucher(request.getVoucher());
        schedule.setDueDate(request.getDueDate());
        schedule.setAmountDue(request.getAmountDue());

        // Default values
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setStatus(PaymentStatus.PENDING);

        repaymentScheduleRepository.save(schedule);
    }

}