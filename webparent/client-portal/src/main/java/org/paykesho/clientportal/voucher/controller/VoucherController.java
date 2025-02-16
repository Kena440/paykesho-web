package org.paykesho.clientportal.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.FileType;
import org.commons.enums.NotificationType;
import org.commons.enums.VoucherStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.VoucherRequestDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.events.enums.Constants;
import org.paykesho.clientportal.files.dto.FileUploadResponse;
import org.paykesho.clientportal.files.service.FileUploadService;
import org.paykesho.clientportal.notifications.service.NotificationService;
import org.paykesho.clientportal.repayments.service.RepaymentService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private RepaymentService repaymentService;

    // Show list of client's vouchers
    @GetMapping("")
    public String showClientVouchers(@AuthenticationPrincipal ClientUserDetails loggedInUser, Model model) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
        if (client == null) {
            throw new PaykeshoError("Client not found.");
        }

        List<Voucher> voucherList = voucherService.getClientVouchers(client);
        model.addAttribute("client", client);
        model.addAttribute("voucherList", voucherList);
        model.addAttribute("pageTitle", "My Vouchers");

        return "vouchers/vouchers";
    }

    // Show form to request a new voucher
    @GetMapping("/request")
    public String showClientVoucherRequestForm(Model model, HttpServletRequest request) {
        Client client = clientService.getAuthenticatedClient(request); // Fetch client by logged-in user
        List<RepaymentSchedule> pendingRepayments = repaymentService.findPendingRepayments(client.getId());

        boolean hasOutstandingDebt = pendingRepayments.stream()
                .anyMatch(schedule -> schedule.getAmountDue().compareTo(schedule.getAmountPaid()) > 0);


        model.addAttribute("voucherRequest", new VoucherRequestDto());
        model.addAttribute("pageTitle", "Voucher Request");
        model.addAttribute("hasOutstandingDebt", hasOutstandingDebt);

        return "vouchers/request";
    }


    @PostMapping("/save")
    public String saveSubmittedRequest(VoucherRequestDto voucherRequestDto, @AuthenticationPrincipal ClientUserDetails loggedInUser, RedirectAttributes redirectAttributes, @RequestParam("payslip") MultipartFile multipartFile) {

        try {
            Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
            if (client == null) {
                throw new PaykeshoError("Client not found.");
            }

            String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_VCR");
            if (paykeshoRef == null) {
                throw new PaykeshoError("Failed to generate Paykesho reference code.");
            }

            Voucher voucher = new Voucher();
            voucher.setReference(paykeshoRef);
            voucher.setApproved(false);
            voucher.setVoucherStatus(VoucherStatus.PENDING);
            voucher.setReason(voucherRequestDto.getReason());
            voucher.setClient(client);
            voucher.setInterestPercent(15);
            voucher.setAmount(voucherRequestDto.getAmount());
            voucher.setAvailableAmount(voucherRequestDto.getAmount());
            BigDecimal totalRepaymentAmount = calculateTotalRepayment(voucher).setScale(2, RoundingMode.CEILING);
            voucher.setRepaymentAmount(totalRepaymentAmount);
            voucher.setRequestDate(new Date());

            if (!multipartFile.isEmpty()) {
                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));

                if (!isValidFileType(multipartFile)) {
                    throw new PaykeshoError("Invalid file type uploaded.");
                }

                voucher.setPayslip(originalFileName);
                FileUploadResponse uploadResponse = fileUploadService.uploadFile(multipartFile, FileType.payslip, 1, "Payslip file for " + voucher.getReference());

                if (!uploadResponse.isStatus()) {
                    throw new PaykeshoError("File upload failed.");
                }

                voucher.setRemoteFileId(uploadResponse.getFileId());
                voucher.setPayslip(uploadResponse.getFilePath());
                voucher.setPayslipPath(uploadResponse.getFilePath());
            } else {
                voucher.setPayslip(null);
                voucher.setPayslipPath(null);
            }

            Voucher savedVoucher = voucherService.save(voucher);

            sendNotification(savedVoucher);

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Voucher Operation Successful: " + voucher.getReference());

        } catch (PaykeshoError e) {
            redirectAttributes.addFlashAttribute(Constants.ERROR, "Error saving voucher: " + e.getMessage());
            return "redirect:/vouchers";
        }

        return "redirect:/vouchers";
    }

    private BigDecimal calculateTotalRepayment(Voucher voucher) {
        BigDecimal interestAmount = BigDecimal.valueOf(voucher.getInterestPercent() / 100).multiply(voucher.getAmount());
        return voucher.getAmount().add(interestAmount);
    }

    private boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("application/pdf"));
    }


    // Show cancellation form (placeholder)
    @GetMapping("/cancel")
    public String showClientVoucherCancellationForm(Model model) {
        // You can add necessary data for the cancellation form here.
        return "vouchers/voucher_cancellation";
    }

    // Private method to handle sending notifications
    private void sendNotification(Voucher voucher) {
        // Generate messages
        String emailMessage = generateEmailMessage(voucher, "REQUEST");
        String smsMessage = generateSmsMessage(voucher, "REQUEST");

        // Send email notification
        boolean emailSent = notificationService.sendNotification(voucher.getClient().getEmail(), emailMessage, NotificationType.email.defaultDescription());
        if (emailSent) {
            System.out.println("Email sent successfully to " + voucher.getClient().getEmail());
        } else {
            System.err.println("Failed to send email to " + voucher.getClient().getEmail());
        }

        // Send SMS notification
        boolean smsSent = notificationService.sendNotification(voucher.getClient().getPhoneNumber(), smsMessage, NotificationType.sms.defaultDescription());
        if (smsSent) {
            System.out.println("SMS sent successfully to " + voucher.getClient().getPhoneNumber());
        } else {
            System.err.println("Failed to send SMS to " + voucher.getClient().getPhoneNumber());
        }
    }

    // Generate email content based on voucher action type
    private String generateEmailMessage(Voucher voucher, String actionType) {
        StringBuilder message = new StringBuilder("<p>Dear ").append(voucher.getClient().getFullName()).append(",</p>");

        if ("REQUEST".equals(actionType)) {
            message.append("<p>Your voucher request has been submitted successfully. ").append("Please allow some time for processing. ").append("Voucher Reference: ").append(voucher.getReference()).append(".</p>");
        } else {
            message.append("<p>Unknown action type.</p>");
        }

        message.append("<br><p>Thank you,</p><p>Paykesho Team</p>");
        return message.toString();
    }

    // Generate SMS content based on voucher action type
    private String generateSmsMessage(Voucher voucher, String actionType) {
        if ("REQUEST".equals(actionType)) {
            return String.format("Dear %s, your voucher request submitted successfully. Reference: %s. Await processing. Get lelo, Pay Kesho.", voucher.getClient(), voucher.getReference());
        }
        return "Unknown action type.";
    }
}
