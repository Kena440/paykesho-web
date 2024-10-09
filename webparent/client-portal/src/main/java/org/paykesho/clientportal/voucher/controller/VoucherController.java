package org.paykesho.clientportal.voucher.controller;

import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.api.dto.VoucherRequestDto;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.notifications.service.NotificationService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.paykesho.clientportal.voucher.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;
import java.util.List;

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
    public String showClientVoucherRequestForm(Model model) {
        model.addAttribute("voucherRequest", new VoucherRequestDto());
        model.addAttribute("pageTitle", "Voucher Request");
        return "vouchers/request";
    }

    // Handle voucher request submission
    @PostMapping("/save")
    public String saveSubmittedRequest(VoucherRequestDto voucherRequestDto, @AuthenticationPrincipal ClientUserDetails loggedInUser, RedirectAttributes redirectAttributes) throws PaykeshoError {
        Client client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());

        if (client == null) {
            throw new PaykeshoError("Client not found.");
        }

        // Generate Paykesho reference
        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_VCR");
        if (paykeshoRef == null) {
            throw new PaykeshoError("Failed to generate Paykesho reference code.");
        }

        // Set voucher details
        Voucher voucher = new Voucher();
        voucher.setReference(paykeshoRef);
        voucher.setApproved(false);
        voucher.setReason(voucherRequestDto.getReason());
        voucher.setClient(client);
        voucher.setAmount(voucherRequestDto.getAmount());
        voucher.setAvailableAmount(voucherRequestDto.getAmount());
        voucher.setRequestDate(new Date());

        // Save the voucher
        Voucher savedVoucher = voucherService.save(voucher);

        // Send notifications (email and SMS)
        sendNotification(savedVoucher);

        // Add a success message
        redirectAttributes.addFlashAttribute("message", "Voucher request submitted. Kindly wait for processing. #" + paykeshoRef);

        return "redirect:/vouchers";
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
            return String.format("Dear %s, your voucher request submitted successfully. Reference: %s. Await processing. Get lelo, Paykesho.", voucher.getClient(), voucher.getReference());
        }
        return "Unknown action type.";
    }
}
