package org.commons.enums;

public enum MerchantType {
    GROCERY_STORE {
        @Override
        public String defaultDescription() {
            return "GROCERY_STORE";
        }
    }, ELECTRONICS_STORE {
        @Override
        public String defaultDescription() {
            return "ELECTRONICS_STORE";
        }
    }, CLOTHING_STORE {
        @Override
        public String defaultDescription() {
            return "CLOTHING_STORE";
        }
    }, PHARMACY {
        @Override
        public String defaultDescription() {
            return "PHARMACY";
        }
    }, RESTAURANT {
        @Override
        public String defaultDescription() {
            return "RESTAURANT";
        }
    }, CAFE {
        @Override
        public String defaultDescription() {
            return "CAFE";
        }
    }, BOOK_STORE {
        @Override
        public String defaultDescription() {
            return "BOOK_STORE";
        }
    }, FURNITURE_STORE {
        @Override
        public String defaultDescription() {
            return "FURNITURE_STORE";
        }
    }, GAS_STATION {
        @Override
        public String defaultDescription() {
            return "GAS_STATION";
        }
    }, CONVENIENCE_STORE {
        @Override
        public String defaultDescription() {
            return "CONVENIENCE_STORE";
        }
    }, HARDWARE_STORE {
        @Override
        public String defaultDescription() {
            return "HARDWARE_STORE";
        }
    }, BAKERY {
        @Override
        public String defaultDescription() {
            return "BAKERY";
        }
    }, JEWELRY_STORE {
        @Override
        public String defaultDescription() {
            return "JEWELRY_STORE";
        }
    }, TOY_STORE {
        @Override
        public String defaultDescription() {
            return "TOY_STORE";
        }
    }, SPORTS_STORE {
        @Override
        public String defaultDescription() {
            return "SPORTS_STORE";
        }
    }, BEAUTY_SALON {
        @Override
        public String defaultDescription() {
            return "BEAUTY_SALON";
        }
    }, BARBERSHOP {
        @Override
        public String defaultDescription() {
            return "BARBERSHOP";
        }
    }, PET_STORE {
        @Override
        public String defaultDescription() {
            return "PET_STORE";
        }
    }, ART_GALLERY {
        @Override
        public String defaultDescription() {
            return "ART_GALLERY";
        }
    }, FLOWER_SHOP {
        @Override
        public String defaultDescription() {
            return "FLOWER_SHOP";
        }
    };

    public abstract String defaultDescription();
}

