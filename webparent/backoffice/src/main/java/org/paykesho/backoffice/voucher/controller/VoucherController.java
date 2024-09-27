package org.paykesho.backoffice.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.service.ClientService;
import org.paykesho.backoffice.config.Utility;
import org.paykesho.backoffice.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.backoffice.repayments.service.RepaymentService;
import org.paykesho.backoffice.settings.email.EmailSettingBag;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.user.service.UserService;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;
    @Autowired
    private UserService userService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private RepaymentService repaymentService;

    @GetMapping("")
    public String listAllVouchersByPage(Model model, HttpServletRequest request) throws PaykeshoError {
        loadCurrencySetting(request);
        model.addAttribute("pageTitle", "List Vouchers");
        model.addAttribute("voucherList", voucherService.listAll());
        return "vouchers/vouchers";
    }

    @GetMapping("/voucher/{id}")
    public String listAllVouchersByClient(@PathVariable("id") int id, Model model) {
        return "vouchers/vouchers";
    }

    @GetMapping("/view/{id}")
    public String showVoucherViewModal(@PathVariable("id") Integer id, Model model) {
        Voucher voucher = voucherService.getVoucherById(id);
        model.addAttribute("voucher", voucher);
        return "vouchers/voucher_view_modal";
    }

    @GetMapping("/approve/{id}")
    public String approveVoucherRequest(@PathVariable("id") Integer voucherId, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Voucher approvedVoucher = runVoucherApprovalProcess(voucherId, request);
        redirectAttributes.addFlashAttribute("message", "Voucher has been approved #" + approvedVoucher.getReference());
        return "redirect:/vouchers";
    }

    @GetMapping("/adjust")
    public String adjustVoucherRequest(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Voucher adjustedVoucher = runVoucherAdjustmentProcess(request);
        redirectAttributes.addFlashAttribute("message", "Voucher has been adjusted #" + adjustedVoucher.getReference());
        return "redirect:/vouchers";
    }

    @GetMapping("/reject/{id}")
    public String rejectVoucherRequest(@PathVariable("id") Integer voucherId, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Voucher rejectedVoucher = runVoucherRejectionProcess(voucherId, request);
        redirectAttributes.addFlashAttribute("message", "Voucher has been rejected #" + rejectedVoucher.getReference());
        return "redirect:/vouchers";
    }

    private Voucher runVoucherApprovalProcess(Integer voucherId, HttpServletRequest request) throws PaykeshoError {
        Voucher voucher = getVoucherOrThrow(voucherId);
        updateVoucherForApproval(voucher, request);
        Voucher approvedVoucher = voucherService.save(voucher);

        updateClientBalance(approvedVoucher);
        runRepaymentScheduleProcess(approvedVoucher);
        //sendNotification(approvedVoucher, "APPROVAL", null);
        return approvedVoucher;
    }

    private void runRepaymentScheduleProcess(Voucher approvedVoucher) throws PaykeshoError {
        RepaymentScheduleRequest scheduleRequest = new RepaymentScheduleRequest();
        scheduleRequest.setClient(approvedVoucher.getClient());
        repaymentService.createRepaymentSchedule(scheduleRequest);
    }

    private void updateClientBalance(Voucher approvedVoucher) {
        ClientBalance balance = clientService.getClientBalance(approvedVoucher.getClient());

        if (balance == null) {
            balance = new ClientBalance();
            balance.setClient(approvedVoucher.getClient());
            balance.setCurrentBalance(BigDecimal.ZERO);
            balance.setAvailableBalance(BigDecimal.ZERO);
        }

        balance.setCurrentBalance(balance.getCurrentBalance().add(approvedVoucher.getAmount()));
        balance.setAvailableBalance(balance.getAvailableBalance().add(approvedVoucher.getAmount()));
        balance.setLastUpdate(new Date());

        clientService.saveClientBalance(balance);
    }

    private Voucher runVoucherAdjustmentProcess(HttpServletRequest request) throws PaykeshoError {
        int voucherId = Integer.parseInt(request.getParameter("id"));
        Voucher voucher = getVoucherOrThrow(voucherId);
        updateVoucherForAdjustment(voucher, request);
        Voucher adjustedVoucher = voucherService.save(voucher);
        sendNotification(adjustedVoucher, "ADJUSTMENT", voucher.getAvailableAmount());
        return adjustedVoucher;
    }

    private Voucher runVoucherRejectionProcess(Integer voucherId, HttpServletRequest request) throws PaykeshoError {
        Voucher voucher = getVoucherOrThrow(voucherId);
        Voucher rejectedVoucher = voucherService.save(voucher);
        sendNotification(rejectedVoucher, "REJECTION", null);
        return rejectedVoucher;
    }

    private Voucher getVoucherOrThrow(Integer voucherId) throws PaykeshoError {
        Voucher voucher = voucherService.getVoucherById(voucherId);
        if (voucher == null) {
            throw new PaykeshoError("No voucher found");
        }
        return voucher;
    }

    private void updateVoucherForApproval(Voucher voucher, HttpServletRequest request) {
        //voucher.setInterestPercent(Float.parseFloat(request.getParameter("voucherInterestPercent")));
        voucher.setInterestPercent(30);
        BigDecimal totalRepaymentAmount = calculateTotalRepayment(voucher).setScale(2, RoundingMode.CEILING);
        voucher.setRepaymentAmount(totalRepaymentAmount);
        voucher.setApproved(true);
    }

    private void updateVoucherForAdjustment(Voucher voucher, HttpServletRequest request) {
        String repaymentCycle = request.getParameter("voucherRepaymentCycle");
        Date repaymentDate = calculateRepaymentDate(repaymentCycle, voucher);
        BigDecimal adjustedAmount = new BigDecimal(request.getParameter("voucherAdjustedAmount"));
        BigDecimal totalRepaymentAmount = calculateTotalRepayment(request).setScale(2, RoundingMode.CEILING);

        voucher.setRepaymentCycle(repaymentCycle);
        voucher.setAvailableAmount(adjustedAmount);
        voucher.setRepaymentAmount(totalRepaymentAmount);
        voucher.setRepaymentDate(repaymentDate);
        voucher.setApproved(true);
    }

    private void sendNotification(Voucher voucher, String actionType, BigDecimal voucherAmount) {
        try {
            String emailSubject = generateEmailSubject(voucher, actionType);
            String emailMessage = generateEmailMessage(voucher, actionType, voucherAmount);
            String smsMessage = generateSmsMessage(voucher, actionType);

            sendVoucherEmail(voucher.getClient().getEmail(), emailMessage, emailSubject);
            //smsNotificationService.send(new SMSDto(voucher.getClient().getPhoneNumber(), smsMessage));
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private BigDecimal calculateTotalRepayment(Voucher voucher) {
        BigDecimal interestAmount = BigDecimal.valueOf(voucher.getInterestPercent() / 100).multiply(voucher.getAmount());
        return voucher.getAmount().add(interestAmount);
    }

    private BigDecimal calculateTotalRepayment(HttpServletRequest request) {
        BigDecimal adjustedAmount = new BigDecimal(request.getParameter("voucherAdjustedAmount"));
        float interestPercent = Float.parseFloat(request.getParameter("voucherInterestPercent"));
        BigDecimal interestAmount = BigDecimal.valueOf(interestPercent / 100).multiply(adjustedAmount);
        return adjustedAmount.add(interestAmount);
    }

    private Date calculateRepaymentDate(String repaymentCycle, Voucher voucher) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(voucher.getRequestDate());
        calendar.add(Calendar.MONTH, Integer.parseInt(repaymentCycle));
        return calendar.getTime();
    }

    private String generateEmailSubject(Voucher voucher, String actionType) {
        switch (actionType) {
            case "APPROVAL":
                return "Voucher Request Approval #" + voucher.getReference();
            case "REJECTION":
                return "Voucher Request Rejected #" + voucher.getReference();
            case "ADJUSTMENT":
                return "Voucher Request Adjusted #" + voucher.getReference();
            default:
                return "";
        }
    }

    private String generateEmailMessage(Voucher voucher, String actionType, BigDecimal voucherAmount) {
        StringBuilder message = new StringBuilder("<p>Congratulations!</p>");
        switch (actionType) {
            case "APPROVAL":
                message.append("<p>Your voucher request has been approved. ").append("Repayment amount: K").append(voucher.getRepaymentAmount()).append(", Repayment date: ").append(voucher.getRepaymentDate()).append(".</p>");
                break;
            case "REJECTION":
                message.append("<p>Your voucher request has been rejected.</p>");
                break;
            case "ADJUSTMENT":
                message.append("<p>Your voucher request has been adjusted to K").append(voucherAmount).append(".</p>").append("<p>Repayment amount: K").append(voucher.getRepaymentAmount()).append(", Repayment date: ").append(voucher.getRepaymentDate()).append(".</p>");
                break;
        }
        message.append("<br><p>Get lelo, Pay Mailo.</p>");
        return message.toString();
    }

    private String generateSmsMessage(Voucher voucher, String actionType) {
        switch (actionType) {
            case "APPROVAL":
                return String.format("Voucher request approved. Repayment amount: K%s, to be paid on %s. Reference: %s. Get lelo, Pay Mailo.", voucher.getRepaymentAmount(), voucher.getRepaymentDate(), voucher.getReference());
            case "REJECTION":
                return String.format("Voucher request rejected. Reference: %s. Get lelo, Pay Mailo.", voucher.getReference());
            case "ADJUSTMENT":
                return String.format("Voucher request adjusted. Repayment amount: K%s, to be paid on %s. Reference: %s. Get lelo, Pay Mailo.", voucher.getRepaymentAmount(), voucher.getRepaymentDate(), voucher.getReference());


            default:
                return "";
        }
    }

    private void sendVoucherEmail(String toEmail, String emailMessage, String emailSubject) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettings = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettings);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(emailSettings.getFromAddress(), emailSettings.getSenderName());
        helper.setTo(toEmail);
        helper.setSubject(emailSubject);
        helper.setText(emailMessage, true);

        mailSender.send(message);
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
