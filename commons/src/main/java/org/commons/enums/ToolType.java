package org.commons.enums;

public enum ToolType {
    DQA {
        @Override
        public String defaultDescription() {
            return "DQA";
        }
    },
    ASSESSMENT {
        @Override
        public String defaultDescription() {
            return "ASSESSMENT";
        }
    };

    public abstract String defaultDescription();
}

