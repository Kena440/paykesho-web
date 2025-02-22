package org.paykesho.organizationportal.utils;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.paykesho.organizationportal.client.repository.ClientRepository;
import org.paykesho.organizationportal.repayments.repository.PayrollDeductionRepository;
import org.paykesho.organizationportal.repayments.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;

@Slf4j
@Service
public class PaykeshoReferenceService {


    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;
    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;

    public String generateCode(String itemRequest) {
        String reference = "";
        String itemPrefix = "";
        Long count;

        switch (itemRequest) {

            case "ITEM_REQ_CLT":
                itemPrefix = "C";
                count = clientRepository.countAllClients() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_RPS":
                itemPrefix = "R";
                count = repaymentScheduleRepository.countAllRepaymentSchedules() + 1;
                reference = getReference(itemPrefix, count);
                break;
            case "ITEM_REQ_PRD":
                itemPrefix = "P";
                count = payrollDeductionRepository.countAllPayrollDeductions() + 1;
                reference = getReference(itemPrefix, count);
                break;

            default:

                break;
        }

        return reference;
    }

    private String getReference(String itemPrefix, Long count) {
        String reference;

        Date dt = new Date();
        int year = dt.getYear();
        int currentYear = year + 1900;

        String randomCode = RandomString.make(5).toUpperCase(Locale.ENGLISH);
        reference = itemPrefix + currentYear + "" + count + randomCode;
        return reference;
    }

}
