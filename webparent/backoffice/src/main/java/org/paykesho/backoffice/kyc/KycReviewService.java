package org.paykesho.backoffice.kyc;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.commons.entity.client.EmployeeRecord;
import org.commons.entity.kyc.ClientKycApplication;
import org.commons.entity.kyc.KycDocument;
import org.commons.enums.KycStatus;
import org.commons.enums.KycDocumentType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.client.repository.ClientRepository;
import org.paykesho.backoffice.notification.general.NotificationClient;
import org.paykesho.backoffice.organizations.repository.EmployeeRecordRepository;
import org.paykesho.backoffice.utils.PaykeshoReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Back-office review of client registrations (KYC). Approving activates the
 * client's account; rejecting or asking for more information keeps it inactive.
 */
@Slf4j
@Service
@Transactional
public class KycReviewService {

    public static final Set<KycStatus> OPEN = EnumSet.of(KycStatus.SUBMITTED, KycStatus.UNDER_REVIEW, KycStatus.MORE_INFO_REQUIRED);
    public static final Set<KycStatus> DECIDED = EnumSet.of(KycStatus.APPROVED, KycStatus.REJECTED);

    private final KycApplicationRepository applications;
    private final KycDocumentRepository documents;
    private final ClientRepository clients;
    private final EmployeeRecordRepository employeeRecords;
    private final PaykeshoReferenceService references;
    private final NotificationClient notificationClient;

    public KycReviewService(KycApplicationRepository applications, KycDocumentRepository documents,
                            ClientRepository clients, EmployeeRecordRepository employeeRecords,
                            PaykeshoReferenceService references, NotificationClient notificationClient) {
        this.applications = applications;
        this.documents = documents;
        this.clients = clients;
        this.employeeRecords = employeeRecords;
        this.references = references;
        this.notificationClient = notificationClient;
    }

    @Transactional(readOnly = true)
    public List<ClientKycApplication> openApplications() {
        return applications.findByStatusInOldestFirst(OPEN);
    }

    @Transactional(readOnly = true)
    public List<ClientKycApplication> decidedApplications() {
        List<ClientKycApplication> decided = applications.findDecided(DECIDED);
        return decided.size() > 50 ? decided.subList(0, 50) : decided;
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return applications.countByStatusIn(OPEN);
    }

    @Transactional(readOnly = true)
    public ClientKycApplication get(Long id) throws PaykeshoError {
        return applications.findWithClient(id).orElseThrow(() -> new PaykeshoError("Registration not found."));
    }

    @Transactional(readOnly = true)
    public List<KycDocument> documentsFor(Long applicationId) {
        return documents.findByKycApplicationIdOrderByIdAsc(applicationId);
    }

    /** The employer's staff-list entry for this client's NRC, if any. */
    @Transactional(readOnly = true)
    public Optional<EmployeeRecord> staffRecordFor(Client client) {
        if (client.getOrganization() == null || client.getNrc() == null) return Optional.empty();
        return Optional.ofNullable(employeeRecords.findByNrcAndOrganization(client.getNrc(), client.getOrganization()));
    }

    /** True while the latest application for this client is not approved (used to guard manual enabling). */
    @Transactional(readOnly = true)
    public boolean blocksActivation(Integer clientId) {
        return applications.findFirstByClient_IdOrderBySubmittedAtDesc(clientId)
                .map(a -> a.getStatus() != KycStatus.APPROVED)
                .orElse(false);
    }

    public ClientKycApplication markUnderReview(Long id, Integer reviewerId) throws PaykeshoError {
        ClientKycApplication a = getForUpdate(id);
        requireOpen(a);
        if (a.getStatus() == KycStatus.SUBMITTED) {
            a.setStatus(KycStatus.UNDER_REVIEW);
            log.info("KYC application {} opened for review by user {}", id, reviewerId);
        }
        return a;
    }

    public ClientKycApplication approve(Long id, Integer reviewerId, String note) throws PaykeshoError {
        ClientKycApplication a = getForUpdate(id);
        requireOpen(a);
        requireReviewer(reviewerId);
        validateForApproval(a);
        a.setStatus(KycStatus.APPROVED);
        a.setReviewedBy(reviewerId);
        a.setReviewedAt(Instant.now());
        a.setDecisionReason(blankToNull(note));
        applications.save(a);

        Client client = a.getClient();
        if (client.getCode() == null || client.getCode().isBlank()) {
            client.setCode(references.generateCode("ITEM_REQ_CLT"));
        }
        client.setEnabled(true);
        client.setAccountNonLocked(true);
        clients.save(client);
        log.info("KYC application {} approved by user {}; client {} activated", id, reviewerId, client.getId());

        notify(client, "Your Paykesho registration has been approved. You can now sign in and request an advance.");
        return a;
    }

    public ClientKycApplication reject(Long id, Integer reviewerId, String reason) throws PaykeshoError {
        return close(id, reviewerId, reason, KycStatus.REJECTED,
                "Your Paykesho registration was not approved: ");
    }

    /** Keeps the application open but records what is missing. */
    public ClientKycApplication requestMoreInfo(Long id, Integer reviewerId, String reason) throws PaykeshoError {
        ClientKycApplication a = getForUpdate(id);
        requireOpen(a);
        requireReviewer(reviewerId);
        String r = blankToNull(reason);
        if (r == null) throw new PaykeshoError("Please say what information is needed.");
        a.setStatus(KycStatus.MORE_INFO_REQUIRED);
        a.setDecisionReason(r);
        applications.save(a);
        log.info("KYC application {}: more information requested by user {}", id, reviewerId);
        notify(a.getClient(), "Paykesho needs more information to approve your registration: " + r);
        return a;
    }

    private ClientKycApplication close(Long id, Integer reviewerId, String reason, KycStatus status, String message) throws PaykeshoError {
        ClientKycApplication a = getForUpdate(id);
        requireOpen(a);
        requireReviewer(reviewerId);
        String r = blankToNull(reason);
        if (r == null) throw new PaykeshoError("Please give a reason.");
        a.setStatus(status);
        a.setReviewedBy(reviewerId);
        a.setReviewedAt(Instant.now());
        a.setDecisionReason(r);
        applications.save(a);
        Client client = a.getClient();
        client.setEnabled(false);
        clients.save(client);
        log.info("KYC application {} {} by user {}", id, status, reviewerId);
        notify(client, message + r);
        return a;
    }

    /** Refuse activation unless required evidence is attached and staff-list details still match. */
    private void validateForApproval(ClientKycApplication a) throws PaykeshoError {
        Client client = a.getClient();
        List<KycDocument> attached = documents.findByKycApplicationIdOrderByIdAsc(a.getId());
        Set<KycDocumentType> types = EnumSet.noneOf(KycDocumentType.class);
        for (KycDocument d : attached) types.add(d.getDocumentType());
        if (!types.containsAll(EnumSet.of(KycDocumentType.NRC_FRONT, KycDocumentType.NRC_BACK, KycDocumentType.PAYSLIP))) {
            throw new PaykeshoError("Approval blocked: the NRC front, NRC back and latest payslip must be attached.");
        }
        EmployeeRecord staff = staffRecordFor(client).orElseThrow(
                () -> new PaykeshoError("Approval blocked: no employer staff-list record was found for this NRC."));
        String expectedName = a.getFirstName() + " " + a.getLastName();
        boolean nameMatches = staff.getName() != null && (staff.getName().equals(client.getName()) || staff.getName().equals(expectedName));
        if (!nameMatches || !java.util.Objects.equals(staff.getNrc(), client.getNrc())
                || !java.util.Objects.equals(staff.getTpin(), client.getTpin())
                || !java.util.Objects.equals(staff.getManNumber(), client.getManNumber())) {
            throw new PaykeshoError("Approval blocked: applicant details do not match the employer staff list. Resolve the mismatch before approval.");
        }
    }

    private ClientKycApplication getForUpdate(Long id) throws PaykeshoError {
        return applications.findWithClientForUpdate(id)
                .orElseThrow(() -> new PaykeshoError("Registration not found."));
    }

    private static void requireOpen(ClientKycApplication a) throws PaykeshoError {
        if (!a.getStatus().isOpen()) {
            throw new PaykeshoError("This registration has already been " + a.getStatus().name().toLowerCase() + ".");
        }
    }

    private static void requireReviewer(Integer reviewerId) throws PaykeshoError {
        if (reviewerId == null) throw new PaykeshoError("Could not identify the reviewer. Please sign in again.");
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > 1000) t = t.substring(0, 1000);
        return t.isEmpty() ? null : t;
    }

    private void notify(Client client, String message) {
        try {
            if (client.getEmail() != null) notificationClient.sendNotification(client.getEmail(), "<p>" + escape(message) + "</p>", "email");
            if (client.getPhoneNumber() != null) notificationClient.sendNotification(client.getPhoneNumber(), message, "sms");
        } catch (Exception e) {
            log.warn("KYC notification for client {} not queued: {}", client.getId(), e.getMessage());
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
