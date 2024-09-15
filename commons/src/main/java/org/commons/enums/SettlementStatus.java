package org.commons.enums;

public enum SettlementStatus {
    PENDING {
        @Override
        public String defaultDescription() {
            return "PENDING";
        }

    },
    COMPLETED {
        @Override
        public String defaultDescription() {
            return "COMPLETED";
        }

    },
    FAILED {
        @Override
        public String defaultDescription() {
            return "FAILED";
        }
    };

    public abstract String defaultDescription();
}
