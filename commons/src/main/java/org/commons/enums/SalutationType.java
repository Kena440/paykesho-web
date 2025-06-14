package org.commons.enums;

public enum SalutationType {
    MR {
        @Override
        public String defaultDescription() {
            return "Mr";
        }
    },

    MRS {
        @Override
        public String defaultDescription() {
            return "Mrs";
        }
    },

    MISS {
        @Override
        public String defaultDescription() {
            return "Miss";
        }
    },

    MS {
        @Override
        public String defaultDescription() {
            return "Ms";
        }
    },

    DR {
        @Override
        public String defaultDescription() {
            return "Dr";
        }
    },

    PROF {
        @Override
        public String defaultDescription() {
            return "Prof";
        }
    };

    public abstract String defaultDescription();
}
