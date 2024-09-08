package org.commons.enums;

public enum ProcessingStatus {
    PROCESSED {
        @Override
        public String defaultDescription() {
            return "PROCESSED";
        }

    }, FAILED {
        @Override
        public String defaultDescription() {
            return "FAILED";
        }

    };

    public abstract String defaultDescription();
}
