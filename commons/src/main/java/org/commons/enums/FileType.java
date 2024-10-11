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
    };

    public abstract String defaultDescription();
}
