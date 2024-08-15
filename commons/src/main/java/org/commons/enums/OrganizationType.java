package org.commons.enums;

public enum OrganizationType {
    HEALTHCARE {
        @Override
        public String defaultDescription() {
            return "HEALTHCARE";
        }
    }, EDUCATION {
        @Override
        public String defaultDescription() {
            return "EDUCATION";
        }
    }, FINANCE {
        @Override
        public String defaultDescription() {
            return "FINANCE";
        }
    }, MANUFACTURING {
        @Override
        public String defaultDescription() {
            return "MANUFACTURING";
        }
    }, RETAIL {
        @Override
        public String defaultDescription() {
            return "RETAIL";
        }
    }, TECHNOLOGY {
        @Override
        public String defaultDescription() {
            return "TECHNOLOGY";
        }
    }, GOVERNMENT {
        @Override
        public String defaultDescription() {
            return "GOVERNMENT";
        }
    }, NON_PROFIT {
        @Override
        public String defaultDescription() {
            return "NON_PROFIT";
        }
    }, TRANSPORTATION {
        @Override
        public String defaultDescription() {
            return "TRANSPORTATION";
        }
    }, ENERGY {
        @Override
        public String defaultDescription() {
            return "ENERGY";
        }
    }, CONSTRUCTION {
        @Override
        public String defaultDescription() {
            return "CONSTRUCTION";
        }
    }, HOSPITALITY {
        @Override
        public String defaultDescription() {
            return "HOSPITALITY";
        }
    }, TELECOMMUNICATION {
        @Override
        public String defaultDescription() {
            return "TELECOMMUNICATION";
        }
    }, AGRICULTURE {
        @Override
        public String defaultDescription() {
            return "AGRICULTURE";
        }
    };

    public abstract String defaultDescription();
}

