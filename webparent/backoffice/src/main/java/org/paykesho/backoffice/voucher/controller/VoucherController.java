package org.paykesho.backoffice.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.settings.Setting;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.service.ClientService;
import org.paykesho.backoffice.notifications.service.NotificationService;
import org.paykesho.backoffice.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.backoffice.repayments.service.RepaymentService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.user.service.UserService;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
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
    NotificationService notificationService;
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

/*    @GetMapping("/reject/{id}")
    public String rejectVoucherRequest(@PathVariable("id") Integer voucherId, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Voucher rejectedVoucher = runVoucherRejectionProcess(voucherId, request);
        redirectAttributes.addFlashAttribute("message", "Voucher has been rejected #" + rejectedVoucher.getReference());
        return "redirect:/vouchers";
    }*/

    @PostMapping(value = "/reject")
    public String rejectVoucherRequest(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {
        String message = request.getParameter("modalRejectVoucherMessage");
        Integer voucherId = Integer.valueOf(request.getParameter("voucherId"));
        Voucher rejectedVoucher = runVoucherRejectionProcess(voucherId, request, message);
        redirectAttributes.addFlashAttribute("message", "Voucher has been rejected #" + rejectedVoucher.getReference());
        return "redirect:/vouchers";
    }

    private Voucher runVoucherApprovalProcess(Integer voucherId, HttpServletRequest request) throws PaykeshoError {
        Voucher voucher = getVoucherOrThrow(voucherId);
        updateVoucherForApproval(voucher, request);
        Voucher approvedVoucher = voucherService.save(voucher);

        updateClientBalance(approvedVoucher);
        runRepaymentScheduleProcess(approvedVoucher);
        sendNotification(approvedVoucher, "APPROVAL", null, null);
        return approvedVoucher;
    }

    private void runRepaymentScheduleProcess(Voucher approvedVoucher) throws PaykeshoError {
        RepaymentScheduleRequest scheduleRequest = new RepaymentScheduleRequest();
        scheduleRequest.setClient(approvedVoucher.getClient());
        scheduleRequest.setDueDate(new Date());
        scheduleRequest.setAmountDue(approvedVoucher.getRepaymentAmount());

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
        sendNotification(adjustedVoucher, "ADJUSTMENT", voucher.getAvailableAmount(), null);
        return adjustedVoucher;
    }

    private Voucher runVoucherRejectionProcess(Integer voucherId, HttpServletRequest request, String message) throws PaykeshoError {
        Voucher voucher = getVoucherOrThrow(voucherId);
        Voucher rejectedVoucher = voucherService.save(voucher);
        sendNotification(rejectedVoucher, "REJECTION", null, message);
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

    private void sendNotification(Voucher voucher, String actionType, BigDecimal voucherAmount, String message) {
        String emailMessage = generateEmailMessage(voucher, actionType, voucherAmount, message);
        String smsMessage = generateSmsMessage(voucher, actionType, message);

        boolean emailSent = notificationService.sendNotification(voucher.getClient().getEmail(), emailMessage, NotificationType.email.defaultDescription());

        if (emailSent) {
            System.out.println("Email sent successfully to " + voucher.getClient().getEmail());
        } else {
            System.err.println("Failed to send email to " + voucher.getClient().getEmail());
        }

        boolean smsSent = notificationService.sendNotification(voucher.getClient().getPhoneNumber(), smsMessage, NotificationType.sms.defaultDescription());

        if (smsSent) {
            System.out.println("SMS sent successfully to " + voucher.getClient().getPhoneNumber());
        } else {
            System.err.println("Failed to send SMS to " + voucher.getClient().getPhoneNumber());
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

    private String generateEmailMessage(Voucher voucher, String actionType, BigDecimal voucherAmount, String message1) {
        StringBuilder message = new StringBuilder("<p>Greetings!</p>");

        String repaymentAmount = "K" + voucher.getRepaymentAmount();
        //String repaymentDate = voucher.getRepaymentDate().toString();
        String repaymentDate = new Date().toString();
        String voucherReference = voucher.getReference();

        switch (actionType) {
            case "APPROVAL":
                message.append("<p>Your voucher request has been approved. ").append("Repayment amount: ").append(repaymentAmount).append(", Repayment date: ").append(repaymentDate).append(".</p>");
                break;
            case "REJECTION":
                message.append("<p>Your voucher request has been rejected. for the following reason: ").append(message1).append(".</p>");
                break;
            case "ADJUSTMENT":
                message.append("<p>Your voucher request has been adjusted to K").append(voucherAmount).append(".</p>").append("<p>Repayment amount: ").append(repaymentAmount).append(", Repayment date: ").append(repaymentDate).append(".</p>");
                break;
            default:
                message.append("<p>Unknown action type.</p>");
                break;
        }
        message.append("<br><p>Get lelo, Pay Kesho.</p>");
        return message.toString();
    }

    private String generateSmsMessage(Voucher voucher, String actionType, String message) {
        String repaymentAmount = "K" + voucher.getRepaymentAmount();
        //String repaymentDate = voucher.getRepaymentDate().toString();
        String repaymentDate = new Date().toString();
        String voucherReference = voucher.getReference();

        switch (actionType) {
            case "APPROVAL":
                return String.format("Voucher request approved. Repayment amount: %s, to be paid on %s. Reference: %s. Get lelo, Pay Kesho.", repaymentAmount, repaymentDate, voucherReference);
            case "REJECTION":
                return String.format("Voucher request rejected for the following reason: %s. Reference: %s. Get lelo, Pay Kesho.", message, voucherReference);
            case "ADJUSTMENT":
                return String.format("Voucher request adjusted. Repayment amount: %s, to be paid on %s. Reference: %s. Get lelo, Pay Kesho.", repaymentAmount, repaymentDate, voucherReference);
            default:
                return "Unknown action type.";
        }
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
