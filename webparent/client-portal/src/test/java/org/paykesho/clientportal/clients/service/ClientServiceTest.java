package org.paykesho.clientportal.clients.service;

import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.enums.AuthenticationType;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Test
    @DisplayName("Should register client successfully")
    void testRegisterClient_Success() {
        // Given
        String encodedPassword = "$2a$10$encodedPasswordHash";
        String verificationCode = "verification_code_12345";

        try (MockedStatic<SecurityValidator> mockedSecurityValidator = mockStatic(SecurityValidator.class)) {
            mockedSecurityValidator.when(() -> SecurityValidator.validateAndSanitizeClient(any(Client.class))).thenAnswer(invocation -> null);
            when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
            when(clientBalanceRepository.getClientBalanceByClientId(anyInt())).thenReturn(null);
            when(clientBalanceRepository.save(any(ClientBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
                Client client = invocation.getArgument(0);
                client.setId(1);
                return client;
            });

            // When
            clientService.registerClient(testClient);

            // Then
            ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
            verify(clientRepository, times(1)).save(clientCaptor.capture());
            Client savedClient = clientCaptor.getValue();

            assertThat(savedClient.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedClient.isEnabled()).isTrue();
            assertThat(savedClient.isAccountNonLocked()).isTrue();
            assertThat(savedClient.getCreatedTime()).isNotNull();
            assertThat(savedClient.getAuthenticationType()).isEqualTo(AuthenticationType.DATABASE);
            assertThat(savedClient.getVerificationCode()).isNotNull();
            assertThat(savedClient.getVerificationCode().length()).isGreaterThan(0);

            // Verify balance was created
            ArgumentCaptor<ClientBalance> balanceCaptor = ArgumentCaptor.forClass(ClientBalance.class);
            verify(clientBalanceRepository, times(1)).save(balanceCaptor.capture());
            ClientBalance savedBalance = balanceCaptor.getValue();
            assertThat(savedBalance.getClient()).isNotNull();
            assertThat(savedBalance.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(savedBalance.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(savedBalance.getHeldBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    @DisplayName("Should throw exception when security validation fails")
    void testRegisterClient_SecurityValidationFailure() {
        // Given
        SecurityException securityException = new SecurityException("Malicious content detected");
        try (MockedStatic<SecurityValidator> mockedSecurityValidator = mockStatic(SecurityValidator.class)) {
            mockedSecurityValidator.when(() -> SecurityValidator.validateAndSanitizeClient(any(Client.class)))
                    .thenThrow(securityException);

            // When/Then
            assertThatThrownBy(() -> clientService.registerClient(testClient))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to register client")
                    .hasRootCause(securityException);

            verify(clientRepository, never()).save(any());
            verify(clientBalanceRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should encode password during registration")
    void testRegisterClient_PasswordEncoding() {
        // Given
        String encodedPassword = "$2a$10$encodedPasswordHash";
        try (MockedStatic<SecurityValidator> mockedSecurityValidator = mockStatic(SecurityValidator.class)) {
            mockedSecurityValidator.when(() -> SecurityValidator.validateAndSanitizeClient(any(Client.class))).thenAnswer(invocation -> null);
            when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
            when(clientBalanceRepository.getClientBalanceByClientId(anyInt())).thenReturn(null);
            when(clientBalanceRepository.save(any(ClientBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
                Client client = invocation.getArgument(0);
                client.setId(1);
                return client;
            });

            // When
            clientService.registerClient(testClient);

            // Then
            verify(passwordEncoder, times(1)).encode("password123");
            ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
            verify(clientRepository).save(clientCaptor.capture());
            assertThat(clientCaptor.getValue().getPassword()).isEqualTo(encodedPassword);
            assertThat(clientCaptor.getValue().getPassword()).isNotEqualTo("password123");
        }
    }

    @Test
    @DisplayName("Should not create balance if it already exists")
    void testRegisterClient_BalanceAlreadyExists() {
        // Given
        String encodedPassword = "$2a$10$encodedPasswordHash";
        ClientBalance existingBalance = new ClientBalance();
        existingBalance.setId(1);
        existingBalance.setClient(testClient);

        try (MockedStatic<SecurityValidator> mockedSecurityValidator = mockStatic(SecurityValidator.class)) {
            mockedSecurityValidator.when(() -> SecurityValidator.validateAndSanitizeClient(any(Client.class))).thenAnswer(invocation -> null);
            when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
            when(clientBalanceRepository.getClientBalanceByClientId(anyInt())).thenReturn(existingBalance);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
                Client client = invocation.getArgument(0);
                client.setId(1);
                return client;
            });

            // When
            clientService.registerClient(testClient);

            // Then
            verify(clientBalanceRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should handle repository save failure")
    void testRegisterClient_RepositorySaveFailure() {
        // Given
        String encodedPassword = "$2a$10$encodedPasswordHash";
        try (MockedStatic<SecurityValidator> mockedSecurityValidator = mockStatic(SecurityValidator.class)) {
            mockedSecurityValidator.when(() -> SecurityValidator.validateAndSanitizeClient(any(Client.class))).thenAnswer(invocation -> null);
            when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
            when(clientRepository.save(any(Client.class))).thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatThrownBy(() -> clientService.registerClient(testClient))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to register client");

            verify(clientBalanceRepository, never()).save(any());
        }
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
