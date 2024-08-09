package org.commons.enums;

public enum MerchantType {
    GROCERY_STORE {
        @Override
        public String defaultDescription() {
            return "Grocery Store";
        }
    },
    ELECTRONICS_STORE {
        @Override
        public String defaultDescription() {
            return "Electronics Store";
        }
    },
    CLOTHING_STORE {
        @Override
        public String defaultDescription() {
            return "Clothing Store";
        }
    },
    PHARMACY {
        @Override
        public String defaultDescription() {
            return "Pharmacy";
        }
    },
    RESTAURANT {
        @Override
        public String defaultDescription() {
            return "Restaurant";
        }
    },
    CAFE {
        @Override
        public String defaultDescription() {
            return "Cafe";
        }
    },
    BOOK_STORE {
        @Override
        public String defaultDescription() {
            return "Book Store";
        }
    },
    FURNITURE_STORE {
        @Override
        public String defaultDescription() {
            return "Furniture Store";
        }
    },
    GAS_STATION {
        @Override
        public String defaultDescription() {
            return "Gas Station";
        }
    },
    CONVENIENCE_STORE {
        @Override
        public String defaultDescription() {
            return "Convenience Store";
        }
    },
    HARDWARE_STORE {
        @Override
        public String defaultDescription() {
            return "Hardware Store";
        }
    },
    BAKERY {
        @Override
        public String defaultDescription() {
            return "Bakery";
        }
    },
    JEWELRY_STORE {
        @Override
        public String defaultDescription() {
            return "Jewelry Store";
        }
    },
    TOY_STORE {
        @Override
        public String defaultDescription() {
            return "Toy Store";
        }
    },
    SPORTS_STORE {
        @Override
        public String defaultDescription() {
            return "Sports Store";
        }
    },
    BEAUTY_SALON {
        @Override
        public String defaultDescription() {
            return "Beauty Salon";
        }
    },
    BARBERSHOP {
        @Override
        public String defaultDescription() {
            return "Barbershop";
        }
    },
    PET_STORE {
        @Override
        public String defaultDescription() {
            return "Pet Store";
        }
    },
    ART_GALLERY {
        @Override
        public String defaultDescription() {
            return "Art Gallery";
        }
    },
    FLOWER_SHOP {
        @Override
        public String defaultDescription() {
            return "Flower Shop";
        }
    };

    public abstract String defaultDescription();
}

