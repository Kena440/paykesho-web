package org.paykesho.backoffice.idempotency.service;

import org.commons.entity.IdempotencyRecord;
import org.commons.test.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.backoffice.idempotency.repository.IdempotencyRecordRepository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService Unit Tests")
class IdempotencyServiceTest extends BaseUnitTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private String testIdempotencyKey;
    private String testEndpoint;
    private String testHttpMethod;
    private String testRequestBody;
    private String testUserIdentifier;

    @BeforeEach
    void setUp() {
        testIdempotencyKey = UUID.randomUUID().toString();
        testEndpoint = "/api/payments";
        testHttpMethod = "POST";
        testRequestBody = "{\"amount\":1000}";
        testUserIdentifier = "user123";
    }

    @Test
    @DisplayName("Should generate unique idempotency key")
    void testGenerateIdempotencyKey() {
        // When
        String key1 = idempotencyService.generateIdempotencyKey();
        String key2 = idempotencyService.generateIdempotencyKey();

        // Then
        assertThat(key1).isNotNull();
        assertThat(key2).isNotNull();
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    @DisplayName("Should return empty when checking null idempotency key")
    void testCheckIdempotency_NullKey() {
        // When
        Optional<IdempotencyRecord> result = idempotencyService.checkIdempotency(null);

        // Then
        assertThat(result).isEmpty();
        verify(idempotencyRecordRepository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    @DisplayName("Should return empty when checking empty idempotency key")
    void testCheckIdempotency_EmptyKey() {
        // When
        Optional<IdempotencyRecord> result = idempotencyService.checkIdempotency("   ");

        // Then
        assertThat(result).isEmpty();
        verify(idempotencyRecordRepository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    @DisplayName("Should return existing record when idempotency key exists")
    void testCheckIdempotency_ExistingKey() {
        // Given
        IdempotencyRecord existingRecord = new IdempotencyRecord();
        existingRecord.setIdempotencyKey(testIdempotencyKey);
        existingRecord.setStatus("COMPLETED");

        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.of(existingRecord));

        // When
        Optional<IdempotencyRecord> result = idempotencyService.checkIdempotency(testIdempotencyKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getIdempotencyKey()).isEqualTo(testIdempotencyKey);
        assertThat(result.get().getStatus()).isEqualTo("COMPLETED");
        verify(idempotencyRecordRepository, times(1)).findByIdempotencyKey(testIdempotencyKey);
    }

    @Test
    @DisplayName("Should create new idempotency record")
    void testCreateIdempotencyRecord_NewRecord() {
        // Given
        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        IdempotencyRecord result = idempotencyService.createIdempotencyRecord(
                testIdempotencyKey, testEndpoint, testHttpMethod, testRequestBody, testUserIdentifier);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdempotencyKey()).isEqualTo(testIdempotencyKey);
        assertThat(result.getEndpoint()).isEqualTo(testEndpoint);
        assertThat(result.getHttpMethod()).isEqualTo(testHttpMethod);
        assertThat(result.getRequestBody()).isEqualTo(testRequestBody);
        assertThat(result.getUserIdentifier()).isEqualTo(testUserIdentifier);
        assertThat(result.getStatus()).isEqualTo("PROCESSING");
        assertThat(result.getRequestHash()).isNotEmpty();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getExpiresAt()).isNotNull();

        verify(idempotencyRecordRepository, times(1)).findByIdempotencyKey(testIdempotencyKey);
        verify(idempotencyRecordRepository, times(1)).save(any(IdempotencyRecord.class));
    }

    @Test
    @DisplayName("Should increment attempt count when key already exists")
    void testCreateIdempotencyRecord_ExistingKey() {
        // Given
        IdempotencyRecord existingRecord = new IdempotencyRecord();
        existingRecord.setIdempotencyKey(testIdempotencyKey);
        existingRecord.setAttemptCount(1);
        existingRecord.setStatus("PROCESSING");

        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.of(existingRecord));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        IdempotencyRecord result = idempotencyService.createIdempotencyRecord(
                testIdempotencyKey, testEndpoint, testHttpMethod, testRequestBody, testUserIdentifier);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttemptCount()).isEqualTo(2);
        verify(idempotencyRecordRepository, times(1)).findByIdempotencyKey(testIdempotencyKey);
        verify(idempotencyRecordRepository, times(1)).save(existingRecord);
    }

    @Test
    @DisplayName("Should mark record as completed")
    void testMarkAsCompleted() {
        // Given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(testIdempotencyKey);
        record.setStatus("PROCESSING");

        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.of(record));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        idempotencyService.markAsCompleted(testIdempotencyKey, 200, "Success", "Content-Type: application/json");

        // Then
        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository, times(1)).save(captor.capture());
        IdempotencyRecord savedRecord = captor.getValue();
        assertThat(savedRecord.getStatus()).isEqualTo("COMPLETED");
        assertThat(savedRecord.getResponseStatus()).isEqualTo(200);
        assertThat(savedRecord.getResponseBody()).isEqualTo("Success");
        assertThat(savedRecord.getResponseHeaders()).isEqualTo("Content-Type: application/json");
    }

    @Test
    @DisplayName("Should not throw exception when marking non-existent record as completed")
    void testMarkAsCompleted_NonExistentRecord() {
        // Given
        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.empty());

        // When/Then - Should not throw exception
        idempotencyService.markAsCompleted(testIdempotencyKey, 200, "Success", null);
        verify(idempotencyRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark record as failed")
    void testMarkAsFailed() {
        // Given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(testIdempotencyKey);
        record.setStatus("PROCESSING");

        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.of(record));
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        idempotencyService.markAsFailed(testIdempotencyKey, "Error occurred", 500);

        // Then
        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository, times(1)).save(captor.capture());
        IdempotencyRecord savedRecord = captor.getValue();
        assertThat(savedRecord.getStatus()).isEqualTo("FAILED");
        assertThat(savedRecord.getErrorMessage()).isEqualTo("Error occurred");
        assertThat(savedRecord.getResponseStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should cleanup expired records")
    void testCleanupExpiredRecords() {
        // Given
        Date now = new Date();
        when(idempotencyRecordRepository.deleteExpiredRecords(any(Date.class))).thenReturn(5);

        // When
        int deletedCount = idempotencyService.cleanupExpiredRecords();

        // Then
        assertThat(deletedCount).isEqualTo(5);
        verify(idempotencyRecordRepository, times(1)).deleteExpiredRecords(any(Date.class));
    }

    @Test
    @DisplayName("Should delete idempotency record")
    void testDeleteIdempotencyRecord() {
        // Given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(testIdempotencyKey);

        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.of(record));

        // When
        idempotencyService.deleteIdempotencyRecord(testIdempotencyKey);

        // Then
        verify(idempotencyRecordRepository, times(1)).findByIdempotencyKey(testIdempotencyKey);
        verify(idempotencyRecordRepository, times(1)).delete(record);
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existent record")
    void testDeleteIdempotencyRecord_NonExistent() {
        // Given
        when(idempotencyRecordRepository.findByIdempotencyKey(testIdempotencyKey))
                .thenReturn(Optional.empty());

        // When/Then - Should not throw exception
        idempotencyService.deleteIdempotencyRecord(testIdempotencyKey);
        verify(idempotencyRecordRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should validate request hash correctly")
    void testValidateRequestHash_Valid() {
        // Given
        IdempotencyRecord record = new IdempotencyRecord();
        String requestBody = "{\"amount\":1000}";
        
        // Create a record first to get the hash
        when(idempotencyRecordRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        IdempotencyRecord created = idempotencyService.createIdempotencyRecord(
                testIdempotencyKey, testEndpoint, testHttpMethod, requestBody, testUserIdentifier);
        record.setRequestHash(created.getRequestHash());

        // When
        boolean isValid = idempotencyService.validateRequestHash(record, requestBody);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return true when request hash is null or empty")
    void testValidateRequestHash_NullHash() {
        // Given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestHash(null);

        // When
        boolean isValid = idempotencyService.validateRequestHash(record, "{\"amount\":1000}");

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when request hash does not match")
    void testValidateRequestHash_Invalid() {
        // Given
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestHash("different_hash");
        String requestBody = "{\"amount\":1000}";

        // When
        boolean isValid = idempotencyService.validateRequestHash(record, requestBody);

        // Then
        assertThat(isValid).isFalse();
    }
}
