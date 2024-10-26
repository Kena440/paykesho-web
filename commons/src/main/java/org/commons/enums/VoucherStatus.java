package org.commons.enums;

public enum VoucherStatus {
    APPROVED {
        @Override
        public String defaultDescription() {
            return "APPROVED";
        }

    }, REJECTED {
        @Override
        public String defaultDescription() {
            return "REJECTED";
        }

    }, PENDING {
        @Override
        public String defaultDescription() {
            return "PENDING";
        }
    };

    public abstract String defaultDescription();
}
