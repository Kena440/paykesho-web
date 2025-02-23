package org.paykesho.organizationportal.repayments.controller;

import lombok.extern.slf4j.Slf4j;

import org.commons.dto.CardPaymentInitialResponse;
import org.commons.dto.CardPaymentRequest;
import org.commons.dto.MoMoPaymentToPaykeshoRequest;
import org.commons.dto.MoMoPaymentToPaykeshoResponse;
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
import org.paykesho.organizationportal.repayments.service.PaymentServiceClient;
import org.paykesho.organizationportal.repayments.service.RepaymentService;
import org.paykesho.organizationportal.security.OrganizationAdminUserDetails;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
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
    @Autowired
    private PaymentServiceClient paymentServiceClient;

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

        List<RepaymentSchedule> dueSchedules = getDueSchedules(organization);
        model.addAttribute("dueSchedules", dueSchedules);
        model.addAttribute("dueSchedulesTotal", dueSchedules.stream().map(RepaymentSchedule::getAmountDue).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("payrollDeductionList", payrollDeductionList);
        model.addAttribute("repaymentRequest", new RepaymentRequest());
        model.addAttribute("clientList", clientList);
        model.addAttribute("pageTitle", "Payroll Deductions");

        return "repayments/payroll_deductions";
    }

    private List<RepaymentSchedule> getDueSchedules(Organization organization) {
        LocalDate today = LocalDate.now();
        LocalDate endOfNextMonth = today.plusMonths(1).withDayOfMonth(today.plusMonths(1).lengthOfMonth());

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = endOfNextMonth.atTime(23, 59, 59, 999999999); // End of next month, just before midnight

        Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());

        List<RepaymentSchedule> dueSchedules = repaymentService.findByDueDateBetween(organization.getId(), startDate, endDate);
        return dueSchedules;
    }

    @PostMapping("/save-schedule")
    public String createRepaymentSchedule(RepaymentScheduleRequest request) throws PaykeshoError {
        repaymentService.createRepaymentSchedule(request);
        return "redirect:/repayments/schedules";
    }

    @PostMapping("/save-payroll-deductions")
    public String processPayrollDeductions(@AuthenticationPrincipal OrganizationAdminUserDetails loggedInUser, HttpServletRequest request, @RequestParam("paymentMethod") String paymentMethod) throws PaykeshoError {

        // Get the logged-in user and their associated organization
        OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByEmail(loggedInUser.getOrganizationAdmin().getEmail());
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);

        // Fetch the repayment schedules
        List<RepaymentSchedule> dueSchedules = getDueSchedules(organization);

        // Process payroll deductions
        repaymentService.processPayrollDeductions(dueSchedules);

        // Handle the selected payment method
        switch (paymentMethod) {
            case "Cash":
                log.info("Processing Cash payment method");
                break;

            case "Mobile Money":
                log.info("Processing Mobile Money payment method");
                MoMoPaymentToPaykeshoRequest momoRequest = createMomoRequest(dueSchedules, organization);
                MoMoPaymentToPaykeshoResponse momoResponse = paymentServiceClient.initializeMomoPayment(momoRequest);
                log.info("Mobile Money Payment Response: {}", momoResponse);
                break;

            case "Card":
                log.info("Processing Card payment method");
                CardPaymentRequest cardRequest = createCardPaymentRequest(dueSchedules, organization, organizationAdmin);
                CardPaymentInitialResponse cardResponse = paymentServiceClient.initializeCardPayment(cardRequest);

                if (cardResponse != null && cardResponse.getResponse() != null) {
                    String redirectUrl = cardResponse.getResponse().getRedirectUrl();
                    log.info("Redirecting to: {}", redirectUrl);
                    return "redirect:" + redirectUrl; // Redirects user to the payment page
                } else {
                    throw new PaykeshoError("Card payment initialization failed.");
                }

            case "Bank Transfer":
                log.info("Processing Bank Transfer payment method");
                break;

            default:
                throw new PaykeshoError("Invalid payment method selected.");
        }

        // Redirect after processing if not a card payment
        return "redirect:/repayments/payroll-deductions";
    }

    private CardPaymentRequest createCardPaymentRequest(List<RepaymentSchedule> dueSchedules, Organization organization, OrganizationAdmin organizationAdmin) {
        CardPaymentRequest request = new CardPaymentRequest();
        request.setAmount(calculateTotalAmount(dueSchedules).toString());
        request.setReferenceNo(UUID.randomUUID().toString());
        request.setCurrency("ZMW");
        request.setDescription("Repayment request: " + organization.getName());

        // Split full name into first and last name
        String[] nameParts = organizationAdmin.getName().split(" ", 2);
        request.setFirstName(nameParts[0]);
        request.setLastName(nameParts.length > 1 ? nameParts[1] : "");

        request.setAddress(organization.getEmail() != null ? organization.getEmail() : "Unknown Address");
        request.setEmail(organization.getEmail());
        request.setPhone(organization.getPhoneNumber() != null ? organization.getPhoneNumber() : "0000000000");

        request.setCity("Lusaka");
        request.setState("Lusaka");
        request.setZipCode("10101");
        request.setCountry("ZAMBIA");

        return request;
    }


    private MoMoPaymentToPaykeshoRequest createMomoRequest(List<RepaymentSchedule> dueSchedules, Organization organization) {
        // Build the mobile money payment request
        MoMoPaymentToPaykeshoRequest request = new MoMoPaymentToPaykeshoRequest();
        request.setAmount(calculateTotalAmount(dueSchedules).toString());
        request.setReferenceNo(UUID.randomUUID().toString());
        request.setSenderId(organization.getPhoneNumber());
        return request;
    }

    private BigDecimal calculateTotalAmount(List<RepaymentSchedule> dueSchedules) {
        return dueSchedules.stream().map(RepaymentSchedule::getAmountDue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
