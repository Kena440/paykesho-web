package org.paykesho.clientportal.clients.service;

import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.enums.AuthenticationType;
import org.commons.enums.ClientType;
import org.commons.security.SecurityValidator;
import org.commons.test.BaseUnitTest;
import org.commons.test.TestDataBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.clientportal.clients.repository.ClientBalanceRepository;
import org.paykesho.clientportal.clients.repository.ClientRepository;
import org.paykesho.clientportal.kyc.RegistrationKycForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.Validation;
import java.util.Map;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService Registration Unit Tests")
class ClientServiceTest extends BaseUnitTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock(lenient = true)
    private ClientBalanceRepository clientBalanceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = TestDataBuilders.client()
                .withEmail("test@example.com")
                .withName("Test User")
                .withPassword("password123")
                .build();
    }

    // Registration (KYC flow) is covered end to end by e2e-tests/portals/registration.spec.ts:
    // staff-list match, required documents, inactive account until back-office approval.

    @Test
    @DisplayName("Registration without an employer is refused before anything is saved")
    void testRegisterClient_NoEmployer() {
        RegistrationKycForm kyc = new RegistrationKycForm();
        kyc.setFirstName("Test");
        kyc.setLastName("User");
        kyc.setHouseNumber("Plot 1");
        kyc.setRoad("Main Road");
        kyc.setArea("Lusaka");
        testClient.setOrganization(null);
        testClient.setClientType(null);
        try (MockedStatic<SecurityValidator> mockedSecurityValidator = mockStatic(SecurityValidator.class)) {
            mockedSecurityValidator.when(() -> SecurityValidator.validateAndSanitizeClient(any(Client.class))).thenAnswer(invocation -> null);
            ReflectionTestUtils.setField(clientService, "validator",
                    Validation.buildDefaultValidatorFactory().getValidator());

            assertThatThrownBy(() -> clientService.registerClient(testClient, kyc, Map.of()))
                    .isInstanceOf(RegistrationRejectedException.class);

            assertThat(testClient.getClientType()).isEqualTo(ClientType.FORMAL);

            verify(clientRepository, never()).save(any());
            verify(clientBalanceRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Registration without first and last name is refused")
    void testRegisterClient_NoName() {
        assertThatThrownBy(() -> clientService.registerClient(testClient, new RegistrationKycForm(), Map.of()))
                .isInstanceOf(RegistrationRejectedException.class)
                .hasMessageContaining("first and last name");
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Staff-list name candidates include the name without the middle name")
    void testNameCandidates() {
        RegistrationKycForm kyc = new RegistrationKycForm();
        kyc.setFirstName(" Mary ");
        kyc.setMiddleName("Chanda");
        kyc.setLastName("Banda");
        assertThat(ClientService.nameCandidates(kyc)).containsExactly("Mary Chanda Banda", "Mary Banda");
        kyc.setMiddleName(null);
        assertThat(ClientService.nameCandidates(kyc)).containsExactly("Mary Banda");
    }

    @Test
    @DisplayName("Should get client by email")
    void testGetClientByEmail() {
        // Given
        testClient.setId(1);
        when(clientRepository.findByEmail("test@example.com")).thenReturn(testClient);

        // When
        Client result = clientService.getClientByEmail("test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(clientRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return null when client not found by email")
    void testGetClientByEmail_NotFound() {
        // Given
        when(clientRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When
        Client result = clientService.getClientByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isNull();
    }
}
