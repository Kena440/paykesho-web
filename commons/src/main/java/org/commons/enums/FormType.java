package org.commons.enums;

public enum FormType {
    GENERAL {
        @Override
        public String defaultDescription() {
            return "GENERAL";
        }
    },
    PRODUCTS {
        @Override
        public String defaultDescription() {
            return "PRODUCTS";
        }
    };

    public abstract String defaultDescription();
}

