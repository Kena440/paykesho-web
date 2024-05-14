package org.commons.enums;

public enum GenderType {
    MALE {
        @Override
        public String defaultDescription() {
            return "MALE";
        }

    },

    FEMALE {
        @Override
        public String defaultDescription() {
            return "FEMALE";
        }
    };

    public abstract String defaultDescription();
}
