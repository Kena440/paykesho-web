package org.paykesho.clientportal.kyc;

import org.commons.entity.kyc.ClientKycApplication;
import org.commons.enums.KycStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Whether a client has been cleared by back-office KYC review. */
@Service
public class KycStatusService {

    private final ClientKycApplicationRepository applications;

    public KycStatusService(ClientKycApplicationRepository applications) {
        this.applications = applications;
    }

    public Optional<KycStatus> latestStatus(Integer clientId) {
        if (clientId == null) return Optional.empty();
        return applications.findFirstByClient_IdOrderBySubmittedAtDesc(clientId).map(ClientKycApplication::getStatus);
    }

    /**
     * True if the client may be active: their latest KYC application is approved,
     * or they have none (accounts created before KYC review existed).
     */
    public boolean isCleared(Integer clientId) {
        return latestStatus(clientId).map(s -> s == KycStatus.APPROVED).orElse(true);
    }

    /** Message for a client whose account is not active because of KYC review, or null. */
    public String pendingMessage(Integer clientId) {
        return latestStatus(clientId).map(s -> {
            if (s.isOpen()) {
                return s == KycStatus.MORE_INFO_REQUIRED
                        ? "Paykesho needs more information to approve your registration. Please contact support"
                        : "Your registration is being reviewed. You can sign in once Paykesho approves it";
            }
            if (s == KycStatus.REJECTED) {
                return "Your registration was not approved. Please contact support";
            }
            return null;
        }).orElse(null);
    }
}
