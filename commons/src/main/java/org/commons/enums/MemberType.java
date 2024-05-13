package org.membershipcommons.enums;

public enum MemberType {
    HOSPITAL {
        @Override
        public String defaultDescription() {
            return "HOSPITAL";
        }
    }, RURAL_HEALTH_CENTER {
        @Override
        public String defaultDescription() {
            return "RURAL HEALTH CENTER";
        }
    }, COMMUNITY_BASED_PROGRAMME {
        @Override
        public String defaultDescription() {
            return "COMMUNITY BASED PROGRAMME";
        }
    }, TRAINING_SCHOOLS {
        @Override
        public String defaultDescription() {
            return "TRAINING SCHOOL";
        }
    };

    public abstract String defaultDescription();
}
