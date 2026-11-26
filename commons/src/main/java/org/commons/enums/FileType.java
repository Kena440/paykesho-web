package org.commons.enums;

public enum FileType {
    logo {
        @Override
        public String defaultDescription() {
            return "logo";
        }

    },
    payslip {
        @Override
        public String defaultDescription() {
            return "payslip";
        }

    },

    photo {
        @Override
        public String defaultDescription() {
            return "photo";
        }
    },

    nrc {
        @Override
        public String defaultDescription() {
            return "NRC";
        }
    },

    selfie {
        @Override
        public String defaultDescription() {
            return "selfie";
        }
    };

    public abstract String defaultDescription();
}
