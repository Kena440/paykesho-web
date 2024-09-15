package org.paykesho.organizationportal.repayments.controller;

import org.commons.entity.client.Client;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Setting;
import org.commons.enums.PaymentStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.client.service.ClientService;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.organizations.service.OrganizationService;
import org.paykesho.organizationportal.repayments.dto.RepaymentRequest;
import org.paykesho.organizationportal.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.organizationportal.repayments.service.RepaymentService;
import org.paykesho.organizationportal.security.OrganizationAdminUserDetails;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;

@Controller
@RequestMapping("/repayments")
public class RepaymentsController {

    @Autowired
    private SettingService settingService;
    @Autowired
    private RepaymentService repaymentService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private ClientService clientService;

    @GetMapping("/schedules")
    public String showSchedules(@AuthenticationPrincipal OrganizationAdminUserDetails loggedInUser, Model model, HttpServletRequest request) {
        OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByEmail(loggedInUser.getOrganizationAdmin().getEmail());
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
        List<RepaymentSchedule> repaymentScheduleList = repaymentService.getRepaymentSchedules(organization.getId());
        List<Client> clientList = clientService.listAllWithRepaymentSchedules(organization, PaymentStatus.COMPLETED);
        loadCurrencySetting(request);

        RepaymentScheduleRequest repaymentScheduleRequest = new RepaymentScheduleRequest();
        //repaymentScheduleRequest.setDueDate(new Date());
        //repaymentScheduleRequest.setDueDateOnForm(String.valueOf(new Date()));

        model.addAttribute("repaymentScheduleList", repaymentScheduleList);
        model.addAttribute("clientList", clientList);
        model.addAttribute("request", repaymentScheduleRequest);
        model.addAttribute("pageTitle", "Repayment Schedules");

        return "repayments/schedules";
    }

    @GetMapping("/payroll-deductions")
    public String showPayrollDeductions(@AuthenticationPrincipal OrganizationAdminUserDetails loggedInUser, Model model, HttpServletRequest request) {
        OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByEmail(loggedInUser.getOrganizationAdmin().getEmail());
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
        List<PayrollDeduction> payrollDeductionList = repaymentService.getPayrollDeductions(organization.getId());
        List<Client> clientList = clientService.listAllWithRepaymentSchedules(organization, PaymentStatus.COMPLETED);

        loadCurrencySetting(request);
        model.addAttribute("payrollDeductionList", payrollDeductionList);
        model.addAttribute("repaymentRequest", new RepaymentRequest());
        model.addAttribute("clientList", clientList);
        model.addAttribute("pageTitle", "Payroll Deductions");

        return "repayments/payroll_deductions";
    }

    @PostMapping("/save-schedule")
    public String createRepaymentSchedule(RepaymentScheduleRequest request) throws PaykeshoError, ParseException {
   /*     DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        request.setDueDate(dateFormatter.parse(request.getDueDateOnForm()));*/

        repaymentService.createRepaymentSchedule(request);
        return "redirect:/repayments/schedules";
    }

    @PostMapping("/save-payroll-deduction")
    public String processRepayment(RepaymentRequest repaymentRequest) throws PaykeshoError {
        repaymentService.processRepayment(repaymentRequest);
        return "redirect:/repayments/payroll-deductions";

    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
