package org.commons.enums;

public enum PaymentMethod {
    CARD {
        @Override
        public String defaultDescription() {
            return "CARD";
        }

    }, MOBILE_MONEY {
        @Override
        public String defaultDescription() {
            return "MOBILE_MONEY";
        }

    }, BANK_TRANSFER {
        @Override
        public String defaultDescription() {
            return "BANK_TRANSFER";
        }
    };

    public abstract String defaultDescription();
}
