package org.paykesho.clientportal.voucher.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.FileType;
import org.commons.enums.VoucherStatus;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.VoucherRequestDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.events.enums.Constants;
import org.paykesho.clientportal.files.dto.FileUploadResponse;
import org.paykesho.clientportal.files.service.FileUploadService;
import org.paykesho.clientportal.notification.general.NotificationClient;
import org.paykesho.clientportal.repayments.service.RepaymentService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
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

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/vouchers")
public class VoucherController {

    private final ClientService clientService;

    private final VoucherService voucherService;

    private final PaykeshoReferenceService paykeshoReferenceService;

    private final NotificationClient notificationClient;

    private final FileUploadService fileUploadService;

    private final RepaymentService repaymentService;

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
            voucher.setInterestPercent(new BigDecimal("15"));
            voucher.setAmount(voucherRequestDto.getAmount());
            voucher.setRepaymentCycle(voucherRequestDto.getRepaymentCycle());
            voucher.setAvailableAmount(voucherRequestDto.getAmount());
            BigDecimal totalRepaymentAmount = calculateTotalRepayment(voucher).setScale(2, RoundingMode.HALF_EVEN);
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
        return org.commons.utils.MoneyMath.totalRepayment(voucher.getAmount(), voucher.getInterestPercent());
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
        try {
            // Send notifications to client
            sendClientNotifications(voucher);
            
            // Send notifications to employer (organization)
            sendEmployerNotifications(voucher);
        } catch (Exception e) {
            log.error("Error sending voucher request notifications: {}", e.getMessage(), e);
            // Don't throw exception - voucher save should succeed even if notifications fail
        }
    }
    
    private void sendClientNotifications(Voucher voucher) {
        // Send email notification using professional template
        boolean emailSent = notificationClient.sendVoucherNotificationEmail(
            voucher.getClient().getFullName(),
            voucher.getClient().getEmail(),
            voucher.getReference(),
            "REQUEST",
            "Please allow some time for processing",
            voucher.getAmount()
        );
        
        if (emailSent) {
            log.info("Email sent successfully to client: {}", voucher.getClient().getEmail());
        } else {
            log.error("Failed to send email to client: {}", voucher.getClient().getEmail());
        }

        // Send SMS notification to client
        if (voucher.getClient().getPhoneNumber() != null && !voucher.getClient().getPhoneNumber().isEmpty()) {
            String smsMessage = generateSmsMessage(voucher, "REQUEST");
            boolean smsSent = notificationClient.sendNotification(voucher.getClient().getPhoneNumber(), smsMessage, "sms");
            if (smsSent) {
                log.info("SMS sent successfully to client: {}", voucher.getClient().getPhoneNumber());
            } else {
                log.error("Failed to send SMS to client: {}", voucher.getClient().getPhoneNumber());
            }
        }
    }
    
    private void sendEmployerNotifications(Voucher voucher) {
        // Get organization from client
        if (voucher.getClient().getOrganization() == null) {
            log.warn("Client {} has no organization associated. Skipping employer notifications.", voucher.getClient().getEmail());
            return;
        }
        
        var organization = voucher.getClient().getOrganization();
        String organizationName = organization.getName() != null ? organization.getName() : "Employer";
        
        // Prepare employer email message
        String employerEmailMessage = String.format(
            "<p>Dear %s,</p>" +
            "<p>A voucher request has been submitted by your employee: %s</p>" +
            "<p>Voucher Reference: %s</p>" +
            "<p>Amount: %s</p>" +
            "<p>Please allow some time for processing.</p>" +
            "<br><p>Thank you,</p><p>Paykesho Team</p>",
            organizationName,
            voucher.getClient().getFullName(),
            voucher.getReference(),
            voucher.getAmount()
        );
        
        // Send email to employer
        if (organization.getEmail() != null && !organization.getEmail().isEmpty()) {
            boolean emailSent = notificationClient.sendNotification(organization.getEmail(), employerEmailMessage, "email");
            if (emailSent) {
                log.info("Voucher request email sent successfully to employer: {}", organization.getEmail());
            } else {
                log.error("Failed to send voucher request email to employer: {}", organization.getEmail());
            }
        }
        
        // Send SMS to employer
        if (organization.getPhoneNumber() != null && !organization.getPhoneNumber().isEmpty()) {
            String employerSmsMessage = String.format(
                "Voucher request submitted by employee %s. Reference: %s, Amount: %s. Await processing. Get lelo, Pay Kesho.",
                voucher.getClient().getFullName(),
                voucher.getReference(),
                voucher.getAmount()
            );
            boolean smsSent = notificationClient.sendNotification(organization.getPhoneNumber(), employerSmsMessage, "sms");
            if (smsSent) {
                log.info("Voucher request SMS sent successfully to employer: {}", organization.getPhoneNumber());
            } else {
                log.error("Failed to send voucher request SMS to employer: {}", organization.getPhoneNumber());
            }
        }
    }

    // Generate SMS content based on voucher action type
    private String generateSmsMessage(Voucher voucher, String actionType) {
        if ("REQUEST".equals(actionType)) {
            return String.format("Dear %s, your voucher request submitted successfully. Reference: %s. Await processing. Get lelo, Pay Kesho.", voucher.getClient(), voucher.getReference());
        }
        return "Unknown action type.";
    }
}
