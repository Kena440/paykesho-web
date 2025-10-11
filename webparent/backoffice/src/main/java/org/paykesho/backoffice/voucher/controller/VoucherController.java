package org.paykesho.backoffice.voucher.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.settings.Setting;
import org.commons.enums.VoucherStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.service.ClientService;
import org.paykesho.backoffice.notification.general.NotificationClient;
import org.paykesho.backoffice.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.backoffice.repayments.service.RepaymentService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.paykesho.backoffice.idempotency.service.IdempotencyService;
import org.commons.entity.IdempotencyRecord;
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
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {


    private final NotificationClient notificationClient;

    private final VoucherService voucherService;

    private final PaykeshoReferenceService paykeshoReferenceService;

    private final ClientService clientService;

    private final SettingService settingService;

    private final RepaymentService repaymentService;

    private final IdempotencyService idempotencyService;

    @GetMapping("")
    public String listAllVouchersByPage(Model model, HttpServletRequest request) throws PaykeshoError {
        loadCurrencySetting(request);
        model.addAttribute("pageTitle", "List Vouchers");
        model.addAttribute("voucherList", voucherService.listAll());
        return "vouchers/vouchers";
    }

    @GetMapping("/voucher/{uuid}")
    public String listAllVouchersByClient(@PathVariable("uuid") String uuid, Model model) {
        return "vouchers/vouchers";
    }

    @GetMapping("/view/{uuid}")
    public String showVoucherViewModal(@PathVariable("uuid") String uuid, Model model) throws PaykeshoError {
        Voucher voucher = voucherService.getVoucherByUuid(uuid);
        model.addAttribute("voucher", voucher);
        return "vouchers/voucher_view_modal";
    }

    @GetMapping("/approve/{uuid}")
    public String approveVoucherRequest(@PathVariable("uuid") String voucherUuid, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        // IDEMPOTENCY: Generate key for this approval operation
        String idempotencyKey = "VOUCHER_APPROVAL_" + voucherUuid;
        
        // Check if already approved
        Optional<IdempotencyRecord> existing = idempotencyService.checkIdempotency(idempotencyKey);
        if (existing.isPresent() && existing.get().isCompleted()) {
            log.warn("Voucher {} already approved. Skipping duplicate approval.", voucherUuid);
            redirectAttributes.addFlashAttribute("message", "Voucher was already approved");
            return "redirect:/vouchers";
        }
        
        // Create idempotency record
        try {
            idempotencyService.createIdempotencyRecord(
                idempotencyKey,
                "/vouchers/approve/" + voucherUuid,
                "GET",
                voucherUuid,
                "backoffice-admin"
            );
        } catch (Exception e) {
            log.error("Error creating idempotency record: {}", e.getMessage());
        }
        
        try {
            Voucher voucher = voucherService.getVoucherByUuid(voucherUuid);
            Voucher approvedVoucher = runVoucherApprovalProcess(voucher.getId(), request);
            
            // Mark as completed
            idempotencyService.markAsCompleted(
                idempotencyKey, 200,
                "{\"voucherRef\":\"" + approvedVoucher.getReference() + "\"}", null
            );
            
            redirectAttributes.addFlashAttribute("message", "Voucher has been approved #" + approvedVoucher.getReference());
            return "redirect:/vouchers";
        } catch (Exception e) {
            log.error("Error approving voucher: {}", e.getMessage());
            idempotencyService.markAsFailed(idempotencyKey, e.getMessage(), 500);
            throw e;
        }
    }

    @GetMapping("/adjust")
    public String adjustVoucherRequest(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Voucher adjustedVoucher = runVoucherAdjustmentProcess(request);
        redirectAttributes.addFlashAttribute("message", "Voucher has been adjusted #" + adjustedVoucher.getReference());
        return "redirect:/vouchers";
    }


    @PostMapping(value = "/reject")
    public String rejectVoucherRequest(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {
        String message = request.getParameter("modalRejectVoucherMessage");
        Integer voucherId = Integer.valueOf(request.getParameter("voucherId"));
        Voucher rejectedVoucher = runVoucherRejectionProcess(voucherId, request, message);
        redirectAttributes.addFlashAttribute("message", "Voucher has been rejected #" + rejectedVoucher.getReference());
        return "redirect:/vouchers";
    }

    private Voucher runVoucherApprovalProcess(Integer voucherId, HttpServletRequest request) throws PaykeshoError {
        // Retrieve and validate voucher
        Voucher voucher = getVoucherOrThrow(voucherId);

        if (!isVoucherApprovable(voucher)) {
            throw new PaykeshoError("Voucher cannot be approved in its current state.");
        }

        // Update and save voucher
        updateVoucherForApproval(voucher, request);
        Voucher approvedVoucher = voucherService.save(voucher);

        // Post-approval processes
        updateClientBalance(approvedVoucher);
        runRepaymentScheduleProcess(approvedVoucher);
        sendNotification(approvedVoucher, "APPROVAL", null, null);

        return approvedVoucher;
    }

    private boolean isVoucherApprovable(Voucher voucher) {
        return voucher != null && voucher.getVoucherStatus() == VoucherStatus.PENDING;
    }

    private void runRepaymentScheduleProcess(Voucher approvedVoucher) throws PaykeshoError {
        int repaymentCycle = approvedVoucher.getRepaymentCycle(); // Get the cycle (e.g., 3 months, 6 months)
        BigDecimal totalAmount = approvedVoucher.getRepaymentAmount();
        BigDecimal monthlyRepayment = totalAmount.divide(BigDecimal.valueOf(repaymentCycle), 2, RoundingMode.HALF_EVEN);

        Date startDate = new Date(); // Assuming first repayment starts today

        for (int i = 1; i <= repaymentCycle; i++) {
            String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_RPS");
            if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

            RepaymentScheduleRequest scheduleRequest = new RepaymentScheduleRequest();
            scheduleRequest.setReference(paykeshoRef);
            scheduleRequest.setClient(approvedVoucher.getClient());
            scheduleRequest.setVoucher(approvedVoucher);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.MONTH, i);
            Date dueDate = calendar.getTime();

            scheduleRequest.setDueDate(dueDate);
            scheduleRequest.setAmountDue(monthlyRepayment);

            repaymentService.createRepaymentSchedule(scheduleRequest);
        }
    }

    private void updateClientBalance(Voucher approvedVoucher) {
        if (approvedVoucher == null || approvedVoucher.getAmount() == null) {
            throw new IllegalArgumentException("Approved voucher or amount cannot be null.");
        }

        Client client = approvedVoucher.getClient();
        ClientBalance balance = clientService.getClientBalance(client);

        if (balance == null) {
            balance = new ClientBalance();
            balance.setClient(client);
            balance.setCurrentBalance(BigDecimal.ZERO);
            balance.setAvailableBalance(BigDecimal.ZERO);
        }

        BigDecimal updatedBalance = balance.getCurrentBalance().add(approvedVoucher.getAmount());

        balance.setCurrentBalance(updatedBalance);
        balance.setAvailableBalance(updatedBalance);
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
        voucher.setVoucherStatus(VoucherStatus.REJECTED);
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
        voucher.setInterestPercent(15);
        BigDecimal totalRepaymentAmount = calculateTotalRepayment(voucher).setScale(2, RoundingMode.HALF_EVEN);
        voucher.setRepaymentAmount(totalRepaymentAmount);
        voucher.setApproved(true);
        voucher.setVoucherStatus(VoucherStatus.APPROVED);
    }

    private void updateVoucherForAdjustment(Voucher voucher, HttpServletRequest request) {
        Integer repaymentCycle = Integer.valueOf(request.getParameter("voucherRepaymentCycle"));
        BigDecimal adjustedAmount = new BigDecimal(request.getParameter("voucherAdjustedAmount"));
        BigDecimal totalRepaymentAmount = calculateTotalRepayment(request).setScale(2, RoundingMode.HALF_EVEN);

        voucher.setRepaymentCycle(repaymentCycle);
        voucher.setAvailableAmount(adjustedAmount);
        voucher.setRepaymentAmount(totalRepaymentAmount);
        voucher.setApproved(true);
        voucher.setVoucherStatus(VoucherStatus.APPROVED);
    }

    private void sendNotification(Voucher voucher, String actionType, BigDecimal voucherAmount, String message) {
        String emailMessage = generateEmailMessage(voucher, actionType, voucherAmount, message);
        String smsMessage = generateSmsMessage(voucher, actionType, message);

        boolean emailSent = notificationClient.sendNotification(voucher.getClient().getEmail(), emailMessage, "email");

        if (emailSent) {
            log.info("Email sent successfully to {}", voucher.getClient().getEmail());
        } else {
            log.error("Failed to send email to " + voucher.getClient().getEmail());
        }

        boolean smsSent = notificationClient.sendNotification(voucher.getClient().getPhoneNumber(), smsMessage, "sms");

        if (smsSent) {
            log.info("SMS sent successfully to {}", voucher.getClient().getPhoneNumber());
        } else {
            log.error("Failed to send SMS to " + voucher.getClient().getPhoneNumber());
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

    private String generateEmailMessage(Voucher voucher, String actionType, BigDecimal voucherAmount, String message1) {
        StringBuilder message = new StringBuilder("<p>Greetings!</p>");

        String repaymentAmount = "K" + voucher.getRepaymentAmount();
        String repaymentDate = new Date().toString();

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

        return switch (actionType) {
            case "APPROVAL" ->
                    String.format("Voucher request approved. Repayment amount: %s, to be paid on %s. Reference: %s. Get lelo, Pay Kesho.", repaymentAmount, repaymentDate, voucherReference);
            case "REJECTION" ->
                    String.format("Voucher request rejected for the following reason: %s. Reference: %s. Get lelo, Pay Kesho.", message, voucherReference);
            case "ADJUSTMENT" ->
                    String.format("Voucher request adjusted. Repayment amount: %s, to be paid on %s. Reference: %s. Get lelo, Pay Kesho.", repaymentAmount, repaymentDate, voucherReference);
            default -> "Unknown action type.";
        };
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
