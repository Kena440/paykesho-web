package org.commons.enums;

public enum SalutationType {
    MR {
        @Override
        public String defaultDescription() {
            return "MR";
        }
    },

    MRS {
        @Override
        public String defaultDescription() {
            return "MRS";
        }
    },

    MISS {
        @Override
        public String defaultDescription() {
            return "MISS";
        }
    },

    MS {
        @Override
        public String defaultDescription() {
            return "MS";
        }
    },

    DR {
        @Override
        public String defaultDescription() {
            return "DR";
        }
    },

    PROF {
        @Override
        public String defaultDescription() {
            return "PROF";
        }
    },

    REV {
        @Override
        public String defaultDescription() {
            return "REV";
        }
    };

    public abstract String defaultDescription();
}
