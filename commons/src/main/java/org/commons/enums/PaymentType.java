package org.commons.enums;

public enum PaymentType {
    ORG_REPAYMENT {
        @Override
        public String defaultDescription() {
            return "ORG_REPAYMENT";
        }

    },
    MERCHANT_SETTLEMENT {
        @Override
        public String defaultDescription() {
            return "MERCHANT_SETTLEMENT";
        }

    },
    OTHER {
        @Override
        public String defaultDescription() {
            return "OTHER";
        }

    };

    public abstract String defaultDescription();
}
