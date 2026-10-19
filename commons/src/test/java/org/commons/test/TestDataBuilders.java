package org.commons.test;

import org.commons.entity.User;
import org.commons.entity.Voucher;
import org.commons.entity.client.Client;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.organization.Organization;
import org.commons.enums.AuthenticationType;
import org.commons.enums.VoucherStatus;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Test data builders for creating test entities.
 * Uses Builder pattern for fluent test data creation.
 */
public class TestDataBuilders {

    public static class ClientBuilder {
        private Integer id;
        private String email = "test@example.com";
        private String name = "Test User";
        private String password = "password123";
        private String code = "TEST_CLT_001";
        private boolean enabled = true;
        private AuthenticationType authenticationType = AuthenticationType.DATABASE;

        public ClientBuilder withId(Integer id) {
            this.id = id;
            return this;
        }

        public ClientBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public ClientBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ClientBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public ClientBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ClientBuilder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ClientBuilder withAuthenticationType(AuthenticationType type) {
            this.authenticationType = type;
            return this;
        }

        public Client build() {
            Client client = new Client();
            if (id != null) {
                client.setId(id);
            }
            client.setEmail(email);
            client.setName(name);
            client.setPassword(password);
            client.setCode(code);
            client.setEnabled(enabled);
            client.setAuthenticationType(authenticationType);
            client.setCreatedTime(new Date());
            client.setAccountNonLocked(true);
            // Set required fields
            client.setPhoneNumber("+260971234567");
            client.setAddress("123 Test Street");
            client.setTpin("1234567890");
            client.setManNumber("1234567890");
            client.setNrc("123456/78/9");
            client.setClientIncome(new BigDecimal("5000.00"));
            client.setFailedAttempt(0);
            return client;
        }
    }

    public static class VoucherBuilder {
        private Integer id;
        private String reference = "TEST_VCR_001";
        private Client client;
        private BigDecimal amount = new BigDecimal("1000.00");
        private BigDecimal availableAmount;
        private VoucherStatus status = VoucherStatus.PENDING;
        private boolean approved = false;
        private java.math.BigDecimal interestPercent = new java.math.BigDecimal("15");

        public VoucherBuilder withId(Integer id) {
            this.id = id;
            return this;
        }

        public VoucherBuilder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public VoucherBuilder withClient(Client client) {
            this.client = client;
            return this;
        }

        public VoucherBuilder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public VoucherBuilder withAvailableAmount(BigDecimal availableAmount) {
            this.availableAmount = availableAmount;
            return this;
        }

        public VoucherBuilder withStatus(VoucherStatus status) {
            this.status = status;
            return this;
        }

        public VoucherBuilder withApproved(boolean approved) {
            this.approved = approved;
            return this;
        }

        public VoucherBuilder withInterestPercent(java.math.BigDecimal interestPercent) {
            this.interestPercent = interestPercent;
            return this;
        }

        public Voucher build() {
            Voucher voucher = new Voucher();
            if (id != null) {
                voucher.setId(id);
            }
            voucher.setReference(reference);
            voucher.setClient(client);
            voucher.setAmount(amount);
            voucher.setAvailableAmount(availableAmount != null ? availableAmount : amount);
            voucher.setVoucherStatus(status);
            voucher.setApproved(approved);
            voucher.setInterestPercent(interestPercent);
            voucher.setRequestDate(new Date());
            voucher.setReason("Test voucher");
            return voucher;
        }
    }

    public static class UserBuilder {
        private Integer id;
        private String email = "admin@example.com";
        private String firstName = "Admin";
        private String lastName = "User";
        private String password = "password123";
        private boolean enabled = true;

        public UserBuilder withId(Integer id) {
            this.id = id;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public User build() {
            User user = new User(email, password, firstName, lastName);
            if (id != null) {
                user.setId(id);
            }
            user.setEnabled(enabled);
            return user;
        }
    }

    public static class MerchantBuilder {
        private Integer id;
        private String name = "Test Merchant";
        private String code = "TEST_MERCHANT";
        private boolean enabled = true;

        public MerchantBuilder withId(Integer id) {
            this.id = id;
            return this;
        }

        public MerchantBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public MerchantBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public MerchantBuilder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Merchant build() {
            Merchant merchant = new Merchant();
            if (id != null) {
                merchant.setId(id);
            }
            merchant.setName(name);
            merchant.setCode(code);
            merchant.setEnabled(enabled);
            merchant.setDetails("Test merchant details");
            return merchant;
        }
    }

    public static class OrganizationBuilder {
        private Integer id;
        private String name = "Test Organization";
        private boolean enabled = true;

        public OrganizationBuilder withId(Integer id) {
            this.id = id;
            return this;
        }

        public OrganizationBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public OrganizationBuilder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Organization build() {
            Organization organization = new Organization();
            if (id != null) {
                organization.setId(id);
            }
            organization.setName(name);
            organization.setEnabled(enabled);
            return organization;
        }
    }

    // Static factory methods
    public static ClientBuilder client() {
        return new ClientBuilder();
    }

    public static VoucherBuilder voucher() {
        return new VoucherBuilder();
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static MerchantBuilder merchant() {
        return new MerchantBuilder();
    }

    public static OrganizationBuilder organization() {
        return new OrganizationBuilder();
    }
}
