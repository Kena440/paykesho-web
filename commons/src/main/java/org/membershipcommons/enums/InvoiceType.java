package org.membershipcommons.enums;

public enum InvoiceType {
    BULK {
        @Override
        public String defaultDescription() {
            return "BULK";
        }
    }, SINGLE {
        @Override
        public String defaultDescription() {
            return "SINGLE";
        }
    };

    public abstract String defaultDescription();
}
