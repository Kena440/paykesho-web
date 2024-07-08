package org.commons.enums;

public enum QuestionType {
    FREETEXT {
        @Override
        public String defaultDescription() {
            return "FREETEXT";
        }
    },
    MULTIPLE_CHOICE {
        @Override
        public String defaultDescription() {
            return "MULTIPLE_CHOICE";
        }
    },
    TRUE_FALSE {
        @Override
        public String defaultDescription() {
            return "TRUE_FALSE";
        }
    },
    CHECKBOX {
        @Override
        public String defaultDescription() {
            return "CHECKBOX";
        }
    },
    MATCHING {
        @Override
        public String defaultDescription() {
            return "MATCHING";
        }
    },
    SHORT_ANSWER {
        @Override
        public String defaultDescription() {
            return "SHORT_ANSWER";
        }
    },
    ESSAY {
        @Override
        public String defaultDescription() {
            return "ESSAY";
        }
    },
    PRODUCT {
        @Override
        public String defaultDescription() {
            return "PRODUCT";
        }
    },
    EQUIPMENT {
        @Override
        public String defaultDescription() {
            return "EQUIPMENT";
        }
    };

    public abstract String defaultDescription();
}

