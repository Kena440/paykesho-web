package org.membershipcommons.enums;

public enum AttendeeType {
    DELEGATE {
        @Override
        public String defaultDescription() {
            return "DELEGATE";
        }

    },

    CHURCH_LEADER {
        @Override
        public String defaultDescription() {
            return "CHURCH_LEADER";
        }
    },

    BOARD_MEMBER {
        @Override
        public String defaultDescription() {
            return "BOARD_MEMBER";
        }
    },

    CHAZ_ED {
        @Override
        public String defaultDescription() {
            return "CHAZ_ED";
        }
    };

    public abstract String defaultDescription();
}
