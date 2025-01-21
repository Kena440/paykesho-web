package org.paykesho.clientportal.repayments.controller;

import org.commons.entity.client.Client;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.repayments.service.RepaymentService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/repayments")
public class RepaymentsController {

    @Autowired
    private SettingService settingService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private RepaymentService repaymentService;

    @GetMapping("/schedules")
    public String showSchedules(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model, HttpServletRequest request) {
        Client client = clientService.getAuthenticatedClient(request);
        List<RepaymentSchedule> repaymentScheduleList = repaymentService.getRepaymentSchedules(client.getId());
        loadCurrencySetting(request);
        model.addAttribute("repaymentScheduleList", repaymentScheduleList);
        model.addAttribute("pageTitle", "Repayment Schedules");

        return "repayments/schedules";
    }

    @GetMapping("/payroll-deductions")
    public String showPayrollDeductions(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model, HttpServletRequest request) throws PaykeshoError {
        Client client = clientService.getAuthenticatedClient(request);
        List<PayrollDeduction> payrollDeductionList = repaymentService.getPayrollDeductions(client);
        loadCurrencySetting(request);
        model.addAttribute("payrollDeductionList", payrollDeductionList);
        model.addAttribute("pageTitle", "Payroll Deductions");

        return "repayments/payroll_deductions";
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
