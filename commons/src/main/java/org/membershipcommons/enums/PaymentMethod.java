package org.membershipcommons.enums;

public enum PaymentMethod {

    CASH {
        @Override
        public String defaultDescription() {
            return "CASH";
        }
    },

    CARD {
        @Override
        public String defaultDescription() {
            return "CARD";
        }
    },

    BANK_TRANSFER {
        @Override
        public String defaultDescription() {
            return "BANK TRANSFER";
        }
    },

    CHEQUE {
        @Override
        public String defaultDescription() {
            return "CHEQUE";
        }
    };

    public abstract String defaultDescription();

}
