package org.paykesho.backoffice.repayments.service;

import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.PaymentStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.backoffice.repayments.repository.PayrollDeductionRepository;
import org.paykesho.backoffice.repayments.repository.RepaymentScheduleRepository;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class RepaymentService {

    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;

    public List<RepaymentSchedule> getRepaymentSchedules() {
        return repaymentScheduleRepository.findAll();
    }

    public List<PayrollDeduction> getPayrollDeductions() {
        return payrollDeductionRepository.findAll();
    }

    public void createRepaymentSchedule(RepaymentScheduleRequest request) throws PaykeshoError {
        BigDecimal amountDue = request.getAmountDue();
        // Date dueDate = request.getDueDate();

        RepaymentSchedule schedule = new RepaymentSchedule();

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_RPS");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        //BigDecimal totals = vouchersService.getApprovedAndValidClientVouchers(request.getClient()).stream().map(Voucher::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        schedule.setReference(paykeshoRef);
        schedule.setClient(request.getClient());
        schedule.setAmountDue(request.getAmountDue());
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setDueDate(new Date());
        schedule.setStatus(PaymentStatus.PENDING);

        repaymentScheduleRepository.save(schedule);
    }
}