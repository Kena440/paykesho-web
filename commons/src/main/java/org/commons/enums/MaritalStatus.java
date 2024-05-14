package org.commons.enums;

public enum MaritalStatus {

    MARRIED {
        @Override
        public String defaultDescription() {
            return "MARRIED";
        }

    },

    SINGLE {
        @Override
        public String defaultDescription() {
            return "SINGLE";
        }
    },

    DIVORCED {
        @Override
        public String defaultDescription() {
            return "DIVORCED";
        }
    },

    WIDOWED {
        @Override
        public String defaultDescription() {
            return "WIDOWED";
        }
    };

    public abstract String defaultDescription();
}
