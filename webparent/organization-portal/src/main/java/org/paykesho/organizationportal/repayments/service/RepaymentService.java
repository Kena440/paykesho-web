package org.paykesho.organizationportal.repayments.service;

import org.commons.entity.Voucher;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.PaymentStatus;
import org.commons.enums.ProcessingStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.client.service.ClientService;
import org.paykesho.organizationportal.repayments.dto.RepaymentRequest;
import org.paykesho.organizationportal.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.organizationportal.repayments.repository.PayrollDeductionRepository;
import org.paykesho.organizationportal.repayments.repository.RepaymentScheduleRepository;
import org.paykesho.organizationportal.utils.PaykeshoReferenceService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;
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
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;
    @Autowired
    private VouchersService vouchersService;
    @Autowired
    private ClientService clientService;

    public void createRepaymentSchedule(RepaymentScheduleRequest request) throws PaykeshoError {
        BigDecimal amountDue = request.getAmountDue();
        // Date dueDate = request.getDueDate();

        RepaymentSchedule schedule = new RepaymentSchedule();

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_RPS");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        BigDecimal totals = vouchersService.getApprovedAndValidClientVouchers(request.getClient()).stream().map(Voucher::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        schedule.setReference(paykeshoRef);
        schedule.setClient(request.getClient());
        schedule.setAmountDue(totals);
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setDueDate(new Date());
        schedule.setStatus(PaymentStatus.PENDING);

        repaymentScheduleRepository.save(schedule);
    }

    public void processRepayment(RepaymentRequest repaymentRequest) throws PaykeshoError {
        RepaymentSchedule schedule = repaymentScheduleRepository.findDistinctFirstByClientIdOrderByIdDesc(repaymentRequest.getClient().getId());

        if (schedule != null && schedule.getStatus() == PaymentStatus.PENDING) {
            BigDecimal repaymentAmount = repaymentRequest.getAmount();
            BigDecimal amountDue = schedule.getAmountDue();
            BigDecimal amountPaid = schedule.getAmountPaid().add(repaymentAmount);

            if (amountPaid.compareTo(amountDue) >= 0) {
                schedule.setAmountPaid(amountDue);
                schedule.setStatus(PaymentStatus.COMPLETED);
            } else {
                schedule.setAmountPaid(amountPaid);
            }

            repaymentScheduleRepository.save(schedule);

            PayrollDeduction payrollDeduction = new PayrollDeduction();

            String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_PRD");
            if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

            payrollDeduction.setReference(paykeshoRef);
            payrollDeduction.setClient(schedule.getClient());
            payrollDeduction.setAmountDeducted(repaymentAmount);
            payrollDeduction.setPayrollDate(new Date());
            payrollDeduction.setStatus(ProcessingStatus.COMPLETED);

            payrollDeductionRepository.save(payrollDeduction);
        }
    }

    public List<RepaymentSchedule> getRepaymentSchedule(Long clientId) {
        return repaymentScheduleRepository.findByClientId(clientId);
    }

    public List<RepaymentSchedule> getRepaymentSchedules(Integer organizationId) {
        return repaymentScheduleRepository.findByClientOrganizationId(organizationId);
    }

    public List<PayrollDeduction> getPayrollDeductions(Integer organizationId) {
        return payrollDeductionRepository.findByClientOrganizationId(organizationId);
    }
}