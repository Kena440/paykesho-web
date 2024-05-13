package org.membershipcommons.enums;

public enum PaymentClass {

    GENERAL {
        @Override
        public String defaultDescription() {
            return "GENERAL";
        }
    },

    MEMBERSHIP {
        @Override
        public String defaultDescription() {
            return "MEMBERSHIP";
        }
    };

    public abstract String defaultDescription();

}
