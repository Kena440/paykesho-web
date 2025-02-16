package org.paykesho.backoffice.repayments.controller;

import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.entity.settings.Setting;
import org.paykesho.backoffice.repayments.service.RepaymentService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RepaymentService repaymentService;


    @GetMapping("/schedules")
    public String showSchedules(Model model, HttpServletRequest request) {

        List<RepaymentSchedule> repaymentScheduleList = repaymentService.getRepaymentSchedules();
        loadCurrencySetting(request);
        model.addAttribute("repaymentScheduleList", repaymentScheduleList);
        model.addAttribute("pageTitle", "Repayment Schedules");

        return "repayments/schedules";
    }

    @GetMapping("/payroll-deductions")
    public String showPayrollDeductions(Model model, HttpServletRequest request) {

        List<PayrollDeduction> payrollDeductionList = repaymentService.getPayrollDeductions();
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
