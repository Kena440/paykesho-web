package org.commons.enums;

public enum PaymentStatus {
    PENDING {
        @Override
        public String defaultDescription() {
            return "PENDING";
        }

    },
    OVERDUE {
        @Override
        public String defaultDescription() {
            return "OVERDUE";
        }

    },
    COMPLETED {
        @Override
        public String defaultDescription() {
            return "COMPLETED";
        }

    },
    PAID {
        @Override
        public String defaultDescription() {
            return "PAID";
        }
    };

    public abstract String defaultDescription();
}
