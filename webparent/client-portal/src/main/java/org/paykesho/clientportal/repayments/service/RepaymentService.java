package org.paykesho.clientportal.repayments.service;

import org.commons.entity.client.Client;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.repayments.repository.PayrollDeductionRepository;
import org.paykesho.clientportal.repayments.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepaymentService {

    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;
    @Autowired
    private ClientService clientService;

    public List<RepaymentSchedule> getRepaymentSchedules(Integer clientId) {
        return repaymentScheduleRepository.findByClientId(clientId);
    }

    public List<PayrollDeduction> getPayrollDeductions(Client client) throws PaykeshoError {
        if (client == null) {
            throw new PaykeshoError("no client id");
        } else {
            return payrollDeductionRepository.findByClient(client);
        }
    }
}