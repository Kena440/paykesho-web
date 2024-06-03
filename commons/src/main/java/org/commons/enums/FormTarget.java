package org.commons.enums;

public enum FormTarget {
    FACILITY {
        @Override
        public String defaultDescription() {
            return "FACILITY";
        }
    },
    DISTRICT {
        @Override
        public String defaultDescription() {
            return "DISTRICT";
        }
    },
    INDIVIDUAL {
        @Override
        public String defaultDescription() {
            return "INDIVIDUAL";
        }
    };

    public abstract String defaultDescription();
}

