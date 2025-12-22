package org.paykesho.organizationportal.repayments.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.Voucher;
import org.commons.entity.client.PayrollDeduction;
import org.commons.entity.client.RepaymentSchedule;
import org.commons.enums.PaymentStatus;
import org.commons.enums.ProcessingStatus;
import org.commons.utils.PaykeshoError;
import org.commons.entity.User;
import org.paykesho.organizationportal.notification.general.NotificationClient;
import org.paykesho.organizationportal.repayments.dto.RepaymentScheduleRequest;
import org.paykesho.organizationportal.repayments.repository.PayrollDeductionRepository;
import org.paykesho.organizationportal.repayments.repository.RepaymentScheduleRepository;
import org.paykesho.organizationportal.user.repository.UserRepository;
import org.paykesho.organizationportal.utils.PaykeshoReferenceService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class RepaymentService {

    @Autowired
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private PayrollDeductionRepository payrollDeductionRepository;
    @Autowired
    private VouchersService vouchersService;
    @Autowired
    private NotificationClient notificationClient;
    @Autowired
    private UserRepository userRepository;

    public void createRepaymentSchedule(RepaymentScheduleRequest request) throws PaykeshoError {
        // Date dueDate = request.getDueDate();

        RepaymentSchedule schedule = new RepaymentSchedule();

        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_RPS");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        BigDecimal totals = vouchersService.getApprovedAndValidClientVouchers(request.getClient()).stream().map(Voucher::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        schedule.setReference(paykeshoRef);
        schedule.setClient(request.getClient());
        schedule.setAmountDue(totals);
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setDueDate(new Date());
        schedule.setStatus(PaymentStatus.PENDING);

        repaymentScheduleRepository.save(schedule);
    }

    public void processPayrollDeductions(List<RepaymentSchedule> dueSchedules) throws PaykeshoError {
        for (RepaymentSchedule schedule : dueSchedules) {
            if (schedule != null && schedule.getStatus() == PaymentStatus.PENDING) {
                BigDecimal amountDue = schedule.getAmountDue();
                BigDecimal amountPaid = schedule.getAmountPaid() != null ? schedule.getAmountPaid() : BigDecimal.ZERO;
                BigDecimal repaymentAmount = schedule.getAmountDue();

                // Update amountPaid and check if the payment is complete
                amountPaid = amountPaid.add(repaymentAmount);

                if (amountPaid.compareTo(amountDue) >= 0) {
                    schedule.setAmountPaid(amountDue);
                    schedule.setStatus(PaymentStatus.COMPLETED);
                } else {
                    schedule.setAmountPaid(amountPaid);
                }

                repaymentScheduleRepository.save(schedule);

                // Create payroll deduction record
                PayrollDeduction payrollDeduction = new PayrollDeduction();

                String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_PRD");
                if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

                payrollDeduction.setReference(paykeshoRef);
                payrollDeduction.setClient(schedule.getClient());
                payrollDeduction.setAmountDeducted(repaymentAmount);
                payrollDeduction.setPayrollDate(new Date());
                payrollDeduction.setStatus(ProcessingStatus.COMPLETED);

                payrollDeductionRepository.save(payrollDeduction);
                
                // Send repayment completion notifications
                sendRepaymentNotifications(schedule, payrollDeduction);
            }
        }
    }
    
    private void sendRepaymentNotifications(RepaymentSchedule schedule, PayrollDeduction payrollDeduction) {
        try {
            var client = schedule.getClient();
            String clientName = client.getName() != null ? client.getName() : client.getEmail();
            
            // Prepare messages
            String emailMessage = String.format(
                "<p>Dear %s,</p>" +
                "<p>Your credit repayment has been successfully processed by your employer.</p>" +
                "<p>Repayment Reference: %s</p>" +
                "<p>Amount Paid: K%s</p>" +
                "<p>Date: %s</p>" +
                "<br><p>Thank you,</p><p>Paykesho Team</p>",
                clientName,
                payrollDeduction.getReference(),
                payrollDeduction.getAmountDeducted(),
                new Date()
            );
            
            String smsMessage = String.format(
                "Dear %s, your credit repayment has been processed. Reference: %s, Amount: K%s. Get lelo, Pay Kesho.",
                clientName,
                payrollDeduction.getReference(),
                payrollDeduction.getAmountDeducted()
            );
            
            // Send notifications to client
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                notificationClient.sendNotification(client.getEmail(), emailMessage, "email");
                log.info("Repayment completion email sent to client: {}", client.getEmail());
            }
            
            if (client.getPhoneNumber() != null && !client.getPhoneNumber().isEmpty()) {
                notificationClient.sendNotification(client.getPhoneNumber(), smsMessage, "sms");
                log.info("Repayment completion SMS sent to client: {}", client.getPhoneNumber());
            }
            
            // Send notifications to Paykesho admins
            // Note: This requires access to backoffice UserService to get admin users
            // For now, we'll send to a configured admin email or implement via service call
            sendAdminNotifications(schedule, payrollDeduction);
            
        } catch (Exception e) {
            log.error("Failed to send repayment completion notifications: {}", e.getMessage(), e);
            // Don't throw exception - repayment processing should succeed even if notifications fail
        }
    }
    
    private void sendAdminNotifications(RepaymentSchedule schedule, PayrollDeduction payrollDeduction) {
        try {
            // Get all enabled admin users directly from database
            List<User> adminUsers = userRepository.findAllEnabledUsers();
            
            if (adminUsers == null || adminUsers.isEmpty()) {
                log.warn("No enabled admin users found. Skipping admin notifications.");
                return;
            }
            
            var client = schedule.getClient();
            String clientName = client.getName() != null ? client.getName() : client.getEmail();
            
            // Prepare admin notification messages
            String adminEmailMessage = String.format(
                "<p>Repayment Completion Notification</p>" +
                "<p>A credit repayment has been successfully processed.</p>" +
                "<p>Client: %s (%s)</p>" +
                "<p>Repayment Reference: %s</p>" +
                "<p>Amount: K%s</p>" +
                "<p>Date: %s</p>" +
                "<br><p>Paykesho Admin Team</p>",
                clientName,
                client.getEmail(),
                payrollDeduction.getReference(),
                payrollDeduction.getAmountDeducted(),
                new Date()
            );
            
            // Send notifications to all enabled admin users
            int successCount = 0;
            int failureCount = 0;
            
            for (var admin : adminUsers) {
                try {
                    // Send email notification
                    if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                        boolean emailSent = notificationClient.sendNotification(admin.getEmail(), adminEmailMessage, "email");
                        if (emailSent) {
                            log.debug("Repayment completion email sent to admin: {}", admin.getEmail());
                            successCount++;
                        } else {
                            log.warn("Failed to send repayment completion email to admin: {}", admin.getEmail());
                            failureCount++;
                        }
                    }
                    
                    // Send SMS notification to admin if phone number is available
                    if (admin.getPhoneNumber() != null && !admin.getPhoneNumber().isEmpty()) {
                        String adminSmsMessage = String.format(
                            "Repayment processed: Client %s, Reference: %s, Amount: K%s. Paykesho Admin.",
                            clientName,
                            payrollDeduction.getReference(),
                            payrollDeduction.getAmountDeducted()
                        );
                        boolean smsSent = notificationClient.sendNotification(admin.getPhoneNumber(), adminSmsMessage, "sms");
                        if (smsSent) {
                            log.debug("Repayment completion SMS sent to admin: {}", admin.getPhoneNumber());
                        } else {
                            log.warn("Failed to send repayment completion SMS to admin: {}", admin.getPhoneNumber());
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Error sending notification to admin {}: {}", admin.getEmail(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            log.info("Repayment completion notifications sent to {} admins ({} successful, {} failed). " +
                    "Client: {}, Reference: {}, Amount: K{}",
                    adminUsers.size(), successCount, failureCount,
                    clientName, payrollDeduction.getReference(), payrollDeduction.getAmountDeducted());
            
        } catch (Exception e) {
            log.error("Failed to send admin notifications for repayment: {}", e.getMessage(), e);
            // Don't throw exception - repayment processing should succeed even if admin notifications fail
        }
    }


    public List<RepaymentSchedule> getRepaymentSchedule(Long clientId) {
        return repaymentScheduleRepository.findByClientId(clientId);
    }

    public List<RepaymentSchedule> getRepaymentSchedules(Integer organizationId) {
        return repaymentScheduleRepository.findByClientOrganizationId(organizationId);
    }

    public List<PayrollDeduction> getPayrollDeductions(Integer organizationId) {
        return payrollDeductionRepository.findByClientOrganizationId(organizationId);
    }

    public List<RepaymentSchedule> findByDueDateBetween(Integer organizationId, Date oneMonthAgo, Date today) {
        return repaymentScheduleRepository.findByDueDateBetween(organizationId, oneMonthAgo, today);
    }

    public Date convertToDate(LocalDate localDate) {
        return java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}