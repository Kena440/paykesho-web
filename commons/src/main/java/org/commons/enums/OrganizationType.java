package org.commons.enums;

public enum OrganizationType {
    HEALTHCARE {
        @Override
        public String defaultDescription() {
            return "Healthcare";
        }
    },
    EDUCATION {
        @Override
        public String defaultDescription() {
            return "Education";
        }
    },
    FINANCE {
        @Override
        public String defaultDescription() {
            return "Finance";
        }
    },
    MANUFACTURING {
        @Override
        public String defaultDescription() {
            return "Manufacturing";
        }
    },
    RETAIL {
        @Override
        public String defaultDescription() {
            return "Retail";
        }
    },
    TECHNOLOGY {
        @Override
        public String defaultDescription() {
            return "Technology";
        }
    },
    GOVERNMENT {
        @Override
        public String defaultDescription() {
            return "Government";
        }
    },
    NON_PROFIT {
        @Override
        public String defaultDescription() {
            return "Non-Profit";
        }
    },
    TRANSPORTATION {
        @Override
        public String defaultDescription() {
            return "Transportation";
        }
    },
    ENERGY {
        @Override
        public String defaultDescription() {
            return "Energy";
        }
    },
    CONSTRUCTION {
        @Override
        public String defaultDescription() {
            return "Construction";
        }
    },
    HOSPITALITY {
        @Override
        public String defaultDescription() {
            return "Hospitality";
        }
    },
    TELECOMMUNICATION {
        @Override
        public String defaultDescription() {
            return "Telecommunication";
        }
    },
    AGRICULTURE {
        @Override
        public String defaultDescription() {
            return "Agriculture";
        }
    };

    public abstract String defaultDescription();
}

