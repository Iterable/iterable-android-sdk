package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InAppTrackingServiceTest {

    private InAppTrackingService trackingService;

    @Mock
    private IterableApi mockIterableApi;

    @Mock
    private IterableInAppManager mockInAppManager;

    @Mock
    private IterableInAppMessage mockMessage;

    @Before
    public void setup() {
        trackingService = new InAppTrackingService(mockIterableApi);
    }

    @Test
    public void trackInAppOpen_shouldCallApi_whenLocationProvided() {
        // Arrange
        String messageId = "test-message-123";
        IterableInAppLocation location = IterableInAppLocation.IN_APP;

        // Act
        trackingService.trackInAppOpen(messageId, location);

        // Assert
        verify(mockIterableApi).trackInAppOpen(messageId, location);
    }

    @Test
    public void trackInAppOpen_shouldUseDefaultLocation_whenLocationIsNull() {
        // Arrange
        String messageId = "test-message-123";

        // Act
        trackingService.trackInAppOpen(messageId, null);

        // Assert
        verify(mockIterableApi).trackInAppOpen(messageId, IterableInAppLocation.IN_APP);
    }

    @Test
    public void trackInAppOpen_shouldNotCrash_whenApiIsNull() {
        // Arrange
        String messageId = "test-message-123";
        InAppTrackingService nullApiService = new InAppTrackingService(null);

        // Act & Assert - should not throw exception
        nullApiService.trackInAppOpen(messageId, IterableInAppLocation.IN_APP);
    }

    // Track In-App Click Tests

    @Test
    public void trackInAppClick_shouldCallApi_whenAllParametersProvided() {
        // Arrange
        String messageId = "test-message-123";
        String url = "https://example.com";
        IterableInAppLocation location = IterableInAppLocation.INBOX;

        // Act
        trackingService.trackInAppClick(messageId, url, location);

        // Assert
        verify(mockIterableApi).trackInAppClick(messageId, url, location);
    }

    @Test
    public void trackInAppClick_shouldUseDefaultLocation_whenLocationIsNull() {
        // Arrange
        String messageId = "test-message-123";
        String url = "https://example.com";

        // Act
        trackingService.trackInAppClick(messageId, url, null);

        // Assert
        verify(mockIterableApi).trackInAppClick(messageId, url, IterableInAppLocation.IN_APP);
    }

    @Test
    public void trackInAppClick_shouldHandleBackButton() {
        // Arrange
        String messageId = "test-message-123";
        String backButton = "itbl://backButton";

        // Act
        trackingService.trackInAppClick(messageId, backButton, IterableInAppLocation.IN_APP);

        // Assert
        verify(mockIterableApi).trackInAppClick(messageId, backButton, IterableInAppLocation.IN_APP);
    }

    // Track In-App Close Tests

    @Test
    public void trackInAppClose_shouldCallApi_whenAllParametersProvided() {
        // Arrange
        String messageId = "test-message-123";
        String url = "https://example.com";
        IterableInAppCloseAction action = IterableInAppCloseAction.LINK;
        IterableInAppLocation location = IterableInAppLocation.IN_APP;

        // Act
        trackingService.trackInAppClose(messageId, url, action, location);

        // Assert
        verify(mockIterableApi).trackInAppClose(messageId, url, action, location);
    }

    @Test
    public void trackInAppClose_shouldUseDefaultLocation_whenLocationIsNull() {
        // Arrange
        String messageId = "test-message-123";
        String url = "https://example.com";
        IterableInAppCloseAction action = IterableInAppCloseAction.LINK;

        // Act
        trackingService.trackInAppClose(messageId, url, action, null);

        // Assert
        verify(mockIterableApi).trackInAppClose(messageId, url, action, IterableInAppLocation.IN_APP);
    }

    @Test
    public void trackInAppClose_shouldHandleBackAction() {
        // Arrange
        String messageId = "test-message-123";
        String backButton = "itbl://backButton";
        IterableInAppCloseAction action = IterableInAppCloseAction.BACK;

        // Act
        trackingService.trackInAppClose(messageId, backButton, action, IterableInAppLocation.IN_APP);

        // Assert
        verify(mockIterableApi).trackInAppClose(messageId, backButton, action, IterableInAppLocation.IN_APP);
    }

    // Remove Message Tests

    @Test
    public void removeMessage_shouldFindAndRemoveMessage_whenMessageExists() {
        // Arrange
        String messageId = "test-message-123";
        when(mockMessage.getMessageId()).thenReturn(messageId);

        when(mockIterableApi.getInAppManager()).thenReturn(mockInAppManager);
        when(mockInAppManager.getMessages()).thenReturn(Arrays.asList(mockMessage));

        // Act
        trackingService.removeMessage(messageId, IterableInAppLocation.INBOX);

        // Assert
        verify(mockInAppManager).removeMessage(
            mockMessage,
            IterableInAppDeleteActionType.INBOX_SWIPE,
            IterableInAppLocation.INBOX
        );
    }

    @Test
    public void removeMessage_shouldUseDefaultLocation_whenLocationIsNull() {
        // Arrange
        String messageId = "test-message-123";
        when(mockMessage.getMessageId()).thenReturn(messageId);

        when(mockIterableApi.getInAppManager()).thenReturn(mockInAppManager);
        when(mockInAppManager.getMessages()).thenReturn(Arrays.asList(mockMessage));

        // Act
        trackingService.removeMessage(messageId, null);

        // Assert
        verify(mockInAppManager).removeMessage(
            mockMessage,
            IterableInAppDeleteActionType.INBOX_SWIPE,
            IterableInAppLocation.IN_APP
        );
    }

    @Test
    public void removeMessage_shouldNotCrash_whenMessageNotFound() {
        // Arrange
        String messageId = "test-message-123";
        String differentId = "different-id-456";
        when(mockMessage.getMessageId()).thenReturn(differentId);

        when(mockIterableApi.getInAppManager()).thenReturn(mockInAppManager);
        when(mockInAppManager.getMessages()).thenReturn(Arrays.asList(mockMessage));

        // Act & Assert - should not throw exception
        trackingService.removeMessage(messageId, IterableInAppLocation.IN_APP);

        // Should not call removeMessage since message wasn't found
        verify(mockInAppManager, never()).removeMessage(any(), any(), any());
    }

    @Test
    public void removeMessage_shouldNotCrash_whenMessagesListIsEmpty() {
        // Arrange
        String messageId = "test-message-123";

        when(mockIterableApi.getInAppManager()).thenReturn(mockInAppManager);
        when(mockInAppManager.getMessages()).thenReturn(Collections.emptyList());

        // Act & Assert - should not throw exception
        trackingService.removeMessage(messageId, IterableInAppLocation.IN_APP);
    }

    @Test
    public void removeMessage_shouldNotCrash_whenApiIsNull() {
        // Arrange
        String messageId = "test-message-123";
        InAppTrackingService nullApiService = new InAppTrackingService(null);

        // Act & Assert - should not throw exception
        nullApiService.removeMessage(messageId, IterableInAppLocation.IN_APP);
    }

    @Test
    public void removeMessage_shouldNotCrash_whenInAppManagerIsNull() {
        // Arrange
        String messageId = "test-message-123";

        when(mockIterableApi.getInAppManager()).thenReturn(null);

        // Act & Assert - should not throw exception
        trackingService.removeMessage(messageId, IterableInAppLocation.IN_APP);
    }

    // Track Screen View Tests

    @Test
    public void trackScreenView_shouldCallTrackWithScreenNameData() {
        // Arrange
        String screenName = "Main Screen";


        // Act
        trackingService.trackScreenView(screenName);

        // Assert
        verify(mockIterableApi).track(eq("Screen Viewed"), any(org.json.JSONObject.class));
    }

    @Test
    public void trackScreenView_shouldNotCrash_whenApiIsNull() {
        // Arrange
        String screenName = "Main Screen";
        InAppTrackingService nullApiService = new InAppTrackingService(null);

        // Act & Assert - should not throw exception
        nullApiService.trackScreenView(screenName);
    }
}

