package org.commons.enums;

/** Kind of document attached to a KYC application (app.kyc_documents.document_type). */
public enum KycDocumentType {
    NRC_FRONT("NRC (front)"),
    NRC_BACK("NRC (back)"),
    PAYSLIP("Latest payslip"),
    SELFIE("Selfie"),
    OTHER("Other");

    private final String label;

    KycDocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
