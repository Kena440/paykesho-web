package org.commons.enums;

public enum ClientType {
    FORMAL {
        @Override
        public String defaultDescription() {
            return "FORMAL";
        }

    },

    INFORMAL {
        @Override
        public String defaultDescription() {
            return "INFORMAL";
        }
    };

    public abstract String defaultDescription();
}
