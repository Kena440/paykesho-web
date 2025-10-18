package org.paykesho.backoffice.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.SecurityAudit;
import org.commons.enums.SecurityAuditStatus;
import org.paykesho.backoffice.security.service.SecurityAuditService;
import org.paykesho.backoffice.security.service.SecurityActionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for Security Audit Dashboard
 * Allows admins to view and take action on security findings
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/security-audit")
public class SecurityAuditController {

    private final SecurityAuditService securityAuditService;
    private final SecurityActionService securityActionService;

    /**
     * Display security audit dashboard
     */
    @GetMapping("")
    public String showSecurityAuditDashboard(Model model) {
        log.debug("Loading security audit dashboard");
        
        List<SecurityAudit> pendingAudits = securityAuditService.getPendingAudits();
        Long pendingCount = securityAuditService.countPending();
        
        model.addAttribute("pendingAudits", pendingAudits);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("pageTitle", "Security Audit Dashboard");
        
        return "security/audit_dashboard";
    }

    /**
     * Run manual security scan
     */
    @PostMapping("/scan-now")
    public String runManualScan(RedirectAttributes redirectAttributes) {
        log.info("Manual security scan initiated by admin");
        
        try {
            securityAuditService.runDailySecurityAudit();
            redirectAttributes.addFlashAttribute("message", "Security scan completed successfully");
        } catch (Exception e) {
            log.error("Error running security scan", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error running security scan: " + e.getMessage());
        }
        
        return "redirect:/security-audit";
    }

    /**
     * View details of a specific audit finding
     */
    @GetMapping("/details/{id}")
    public String viewAuditDetails(@PathVariable Integer id, Model model) {
        SecurityAudit audit = securityAuditService.getAuditById(id);
        
        if (audit == null) {
            return "redirect:/security-audit";
        }
        
        model.addAttribute("audit", audit);
        model.addAttribute("pageTitle", "Security Audit Details");
        
        return "security/audit_details";
    }

    /**
     * Delete malicious record
     */
    @PostMapping("/delete/{id}")
    public String deleteMaliciousRecord(@PathVariable Integer id,
                                       @RequestParam String actionNotes,
                                       RedirectAttributes redirectAttributes) {
        try {
            SecurityAudit audit = securityAuditService.getAuditById(id);
            
            if (audit == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Audit record not found");
                return "redirect:/security-audit";
            }
            
            // Delete the malicious record from its table
            boolean deleted = securityActionService.deleteMaliciousRecord(
                audit.getTableName(), 
                audit.getRecordId()
            );
            
            if (deleted) {
                // Update audit status
                securityActionService.markAsResolved(id, actionNotes);
                redirectAttributes.addFlashAttribute("message", 
                    "Malicious record deleted successfully from " + audit.getTableName());
                log.info("Deleted malicious record: table={}, id={}", audit.getTableName(), audit.getRecordId());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete record");
            }
            
        } catch (Exception e) {
            log.error("Error deleting malicious record", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/security-audit";
    }

    /**
     * Mark as false positive
     */
    @PostMapping("/false-positive/{id}")
    public String markAsFalsePositive(@PathVariable Integer id,
                                     @RequestParam String actionNotes,
                                     RedirectAttributes redirectAttributes) {
        try {
            securityActionService.markAsFalsePositive(id, actionNotes);
            redirectAttributes.addFlashAttribute("message", "Marked as false positive");
        } catch (Exception e) {
            log.error("Error marking as false positive", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/security-audit";
    }

    /**
     * Mark as in progress
     */
    @PostMapping("/in-progress/{id}")
    public String markAsInProgress(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            securityActionService.markAsInProgress(id);
            redirectAttributes.addFlashAttribute("message", "Marked as in progress");
        } catch (Exception e) {
            log.error("Error marking as in progress", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/security-audit";
    }
}

