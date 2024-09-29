package org.commons.enums;

public enum NotificationType {
    sms {
        @Override
        public String defaultDescription() {
            return "sms";
        }

    },
    app {
        @Override
        public String defaultDescription() {
            return "app";
        }

    },

    email {
        @Override
        public String defaultDescription() {
            return "email";
        }
    };

    public abstract String defaultDescription();
}
