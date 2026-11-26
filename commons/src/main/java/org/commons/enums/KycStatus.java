package org.commons.enums;

/** Review state of a client's registration (app.client_kyc_applications.status). */
public enum KycStatus {
    SUBMITTED,
    UNDER_REVIEW,
    MORE_INFO_REQUIRED,
    APPROVED,
    REJECTED;

    /** Still waiting for a back-office decision. */
    public boolean isOpen() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == MORE_INFO_REQUIRED;
    }
}
