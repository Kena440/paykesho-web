package org.paykesho.paymentservice.payments.repositories;

import org.commons.entity.payments.Payment;
import org.commons.enums.PaymentMethod;
import org.commons.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("PaymentRepository Integration Tests")
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setPaymentReference("PAY_REF_001");
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        testPayment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        testPayment.setAmount(new BigDecimal("1000.00"));
        testPayment.setFinancialInstitution("Zynlepay");
        testPayment.setAccountNumber("ACC123");
        testPayment.setPaymentDate(new Date());
    }

    @Test
    @DisplayName("Should save payment successfully")
    void testSavePayment() {
        // When
        Payment saved = paymentRepository.save(testPayment);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPaymentReference()).isEqualTo("PAY_REF_001");
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should find payment by reference")
    void testFindByPaymentReference() {
        // Given
        paymentRepository.save(testPayment);

        // When
        Optional<Payment> found = paymentRepository.findByPaymentReference("PAY_REF_001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentReference()).isEqualTo("PAY_REF_001");
    }

    @Test
    @DisplayName("Should return empty when payment reference not found")
    void testFindByPaymentReference_NotFound() {
        // When
        Optional<Payment> found = paymentRepository.findByPaymentReference("NON_EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }
}
