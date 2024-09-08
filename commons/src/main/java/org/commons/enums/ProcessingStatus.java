package org.commons.enums;

public enum ProcessingStatus {
    PROCESSED {
        @Override
        public String defaultDescription() {
            return "PROCESSED";
        }

    }, COMPLETED {
        @Override
        public String defaultDescription() {
            return "COMPLETED";
        }

    }, FAILED {
        @Override
        public String defaultDescription() {
            return "FAILED";
        }

    };

    public abstract String defaultDescription();
}
