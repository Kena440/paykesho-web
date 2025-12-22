package org.paykesho.notificationservice.service;

import org.commons.entity.NotificationQueue;
import org.commons.enums.NotificationStatus;
import org.commons.enums.NotificationType;
import org.commons.exception.BusinessLogicException;
import org.commons.exception.ResourceNotFoundException;
import org.commons.exception.ValidationException;
import org.commons.test.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paykesho.notificationservice.repository.NotificationQueueRepository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueueService Unit Tests")
class NotificationQueueServiceTest extends BaseUnitTest {

    @Mock
    private NotificationQueueRepository notificationQueueRepository;

    @Mock
    private org.paykesho.notificationservice.notifications.repository.NotificationsRepository notificationsRepository;

    @InjectMocks
    private NotificationQueueService notificationQueueService;

    private String testEmail;
    private String testSubject;
    private String testContent;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testSubject = "Test Subject";
        testContent = "Test notification content";
    }

    @Test
    @DisplayName("Should add notification successfully")
    void testAddNotification_Success() {
        // Given
        NotificationQueue savedNotification = new NotificationQueue();
        savedNotification.setId(1);
        savedNotification.setRecipientEmail(testEmail);
        savedNotification.setSubject(testSubject);
        savedNotification.setContent(testContent);
        savedNotification.setNotificationType(NotificationType.email);
        savedNotification.setStatus(NotificationStatus.PENDING);

        when(notificationQueueRepository.save(any(NotificationQueue.class)))
                .thenReturn(savedNotification);

        // When
        NotificationQueue result = notificationQueueService.addNotification(
                NotificationType.email, testEmail, testSubject, testContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecipientEmail()).isEqualTo(testEmail);
        assertThat(result.getSubject()).isEqualTo(testSubject);
        assertThat(result.getContent()).isEqualTo(testContent);
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.email);
        verify(notificationQueueRepository, times(1)).save(any(NotificationQueue.class));
    }

    @Test
    @DisplayName("Should throw exception when email is null")
    void testAddNotification_NullEmail() {
        // When/Then
        assertThatThrownBy(() -> notificationQueueService.addNotification(
                NotificationType.email, null, testSubject, testContent))
                .isInstanceOf(ValidationException.class);

        verify(notificationQueueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email format is invalid")
    void testAddNotification_InvalidEmail() {
        // When/Then
        assertThatThrownBy(() -> notificationQueueService.addNotification(
                NotificationType.email, "invalid-email", testSubject, testContent))
                .isInstanceOf(ValidationException.class);

        verify(notificationQueueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when subject is null")
    void testAddNotification_NullSubject() {
        // When/Then
        assertThatThrownBy(() -> notificationQueueService.addNotification(
                NotificationType.email, testEmail, null, testContent))
                .isInstanceOf(ValidationException.class);

        verify(notificationQueueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when subject exceeds max length")
    void testAddNotification_SubjectTooLong() {
        // Given
        String longSubject = "a".repeat(256); // Exceeds MAX_SUBJECT_LENGTH (255)

        // When/Then
        assertThatThrownBy(() -> notificationQueueService.addNotification(
                NotificationType.email, testEmail, longSubject, testContent))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Subject must not exceed");
    }

    @Test
    @DisplayName("Should update notification status successfully")
    void testUpdateNotificationStatus_Success() {
        // Given
        Integer notificationId = 1;
        NotificationQueue notification = new NotificationQueue();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRecipientEmail(testEmail);
        notification.setContent(testContent);

        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationQueueRepository.save(any(NotificationQueue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationQueue result = notificationQueueService.updateNotificationStatus(
                notificationId, NotificationStatus.PROCESSING);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        verify(notificationQueueRepository, times(1)).save(any(NotificationQueue.class));
    }

    @Test
    @DisplayName("Should throw exception when notification not found")
    void testUpdateNotificationStatus_NotFound() {
        // Given
        Integer notificationId = 999;
        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> notificationQueueService.updateNotificationStatus(
                notificationId, NotificationStatus.PROCESSING))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationQueueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when trying to update sent notification")
    void testUpdateNotificationStatus_AlreadySent() {
        // Given
        Integer notificationId = 1;
        NotificationQueue notification = new NotificationQueue();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When/Then
        assertThatThrownBy(() -> notificationQueueService.updateNotificationStatus(
                notificationId, NotificationStatus.PROCESSING))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("already been sent");
    }

    @Test
    @DisplayName("Should retry failed notification successfully")
    void testRetryNotification_Success() {
        // Given
        Integer notificationId = 1;
        NotificationQueue notification = new NotificationQueue();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);
        notification.setMaxRetries(3);

        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationQueueRepository.save(any(NotificationQueue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationQueue result = notificationQueueService.retryNotification(notificationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRetryCount()).isEqualTo(2);
        verify(notificationQueueRepository, times(1)).save(any(NotificationQueue.class));
    }

    @Test
    @DisplayName("Should throw exception when notification cannot be retried")
    void testRetryNotification_CannotRetry() {
        // Given
        Integer notificationId = 1;
        NotificationQueue notification = new NotificationQueue();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(3);
        notification.setMaxRetries(3);

        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When/Then
        assertThatThrownBy(() -> notificationQueueService.retryNotification(notificationId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("cannot be retried");
    }

    @Test
    @DisplayName("Should cancel notification successfully")
    void testCancelNotification_Success() {
        // Given
        Integer notificationId = 1;
        NotificationQueue notification = new NotificationQueue();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.PENDING);

        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationQueueRepository.save(any(NotificationQueue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationQueue result = notificationQueueService.cancelNotification(notificationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        verify(notificationQueueRepository, times(1)).save(any(NotificationQueue.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel sent notification")
    void testCancelNotification_AlreadySent() {
        // Given
        Integer notificationId = 1;
        NotificationQueue notification = new NotificationQueue();
        notification.setId(notificationId);
        notification.setStatus(NotificationStatus.SENT);

        when(notificationQueueRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // When/Then
        assertThatThrownBy(() -> notificationQueueService.cancelNotification(notificationId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("already been sent");
    }

    @Test
    @DisplayName("Should get notification by UUID successfully")
    void testGetNotificationByUuid_Success() {
        // Given
        String uuid = UUID.randomUUID().toString();
        NotificationQueue notification = new NotificationQueue();
        notification.setId(1);
        notification.setUuid(uuid);

        when(notificationQueueRepository.findByUuid(uuid)).thenReturn(Optional.of(notification));

        // When
        NotificationQueue result = notificationQueueService.getNotificationByUuid(uuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(uuid);
        verify(notificationQueueRepository, times(1)).findByUuid(uuid);
    }

    @Test
    @DisplayName("Should throw exception when UUID is invalid")
    void testGetNotificationByUuid_InvalidUuid() {
        // When/Then
        assertThatThrownBy(() -> notificationQueueService.getNotificationByUuid("invalid-uuid"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid UUID format");
    }
}
