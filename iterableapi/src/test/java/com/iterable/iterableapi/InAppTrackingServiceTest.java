package com.iterable.iterableapi;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
        IterableInAppLocation location = IterableInAppLocation.IN_APP;

        // Act
        trackingService.trackInAppOpen(mockMessage, location);

        // Assert
        verify(mockIterableApi).trackInAppOpen(mockMessage, location);
    }

    @Test
    public void trackInAppOpen_shouldUseDefaultLocation_whenLocationIsNull() {
        // Act
        trackingService.trackInAppOpen(mockMessage, null);

        // Assert
        verify(mockIterableApi).trackInAppOpen(mockMessage, IterableInAppLocation.IN_APP);
    }

    @Test
    public void trackInAppOpen_shouldNotCrash_whenApiIsNull() {
        // Arrange
        InAppTrackingService nullApiService = new InAppTrackingService(null);

        // Act & Assert - should not throw exception
        nullApiService.trackInAppOpen(mockMessage, IterableInAppLocation.IN_APP);
    }

    // Track In-App Click Tests

    @Test
    public void trackInAppClick_shouldCallApi_whenAllParametersProvided() {
        // Arrange
        String url = "https://example.com";
        IterableInAppLocation location = IterableInAppLocation.INBOX;

        // Act
        trackingService.trackInAppClick(mockMessage, url, location);

        // Assert
        verify(mockIterableApi).trackInAppClick(mockMessage, url, location);
    }

    @Test
    public void trackInAppClick_shouldUseDefaultLocation_whenLocationIsNull() {
        // Arrange
        String url = "https://example.com";

        // Act
        trackingService.trackInAppClick(mockMessage, url, null);

        // Assert
        verify(mockIterableApi).trackInAppClick(mockMessage, url, IterableInAppLocation.IN_APP);
    }

    @Test
    public void trackInAppClick_shouldHandleBackButton() {
        // Arrange
        String backButton = "itbl://backButton";

        // Act
        trackingService.trackInAppClick(mockMessage, backButton, IterableInAppLocation.IN_APP);

        // Assert
        verify(mockIterableApi).trackInAppClick(mockMessage, backButton, IterableInAppLocation.IN_APP);
    }

    // Track In-App Close Tests

    @Test
    public void trackInAppClose_shouldCallApi_whenAllParametersProvided() {
        // Arrange
        String url = "https://example.com";
        IterableInAppCloseAction action = IterableInAppCloseAction.LINK;
        IterableInAppLocation location = IterableInAppLocation.IN_APP;

        // Act
        trackingService.trackInAppClose(mockMessage, url, action, location);

        // Assert
        verify(mockIterableApi).trackInAppClose(mockMessage, url, action, location);
    }

    @Test
    public void trackInAppClose_shouldUseDefaultLocation_whenLocationIsNull() {
        // Arrange
        String url = "https://example.com";
        IterableInAppCloseAction action = IterableInAppCloseAction.LINK;

        // Act
        trackingService.trackInAppClose(mockMessage, url, action, null);

        // Assert
        verify(mockIterableApi).trackInAppClose(mockMessage, url, action, IterableInAppLocation.IN_APP);
    }

    @Test
    public void trackInAppClose_shouldHandleBackAction() {
        // Arrange
        String backButton = "itbl://backButton";
        IterableInAppCloseAction action = IterableInAppCloseAction.BACK;

        // Act
        trackingService.trackInAppClose(mockMessage, backButton, action, IterableInAppLocation.IN_APP);

        // Assert
        verify(mockIterableApi).trackInAppClose(mockMessage, backButton, action, IterableInAppLocation.IN_APP);
    }

    // Remove Message Tests

    @Test
    public void removeMessage_shouldRemoveMessage_whenMarkedForDeletionAndNotConsumed() {
        // Arrange
        when(mockMessage.isMarkedForDeletion()).thenReturn(true);
        when(mockMessage.isConsumed()).thenReturn(false);
        when(mockIterableApi.getInAppManager()).thenReturn(mockInAppManager);

        // Act
        trackingService.removeMessage(mockMessage);

        // Assert
        verify(mockInAppManager).removeMessage(mockMessage);
    }

    @Test
    public void removeMessage_shouldNotRemove_whenNotMarkedForDeletion() {
        // Arrange
        when(mockMessage.isMarkedForDeletion()).thenReturn(false);

        // Act
        trackingService.removeMessage(mockMessage);

        // Assert
        verify(mockInAppManager, never()).removeMessage(any(IterableInAppMessage.class));
    }

    @Test
    public void removeMessage_shouldNotRemove_whenAlreadyConsumed() {
        // Arrange
        when(mockMessage.isMarkedForDeletion()).thenReturn(true);
        when(mockMessage.isConsumed()).thenReturn(true);

        // Act
        trackingService.removeMessage(mockMessage);

        // Assert
        verify(mockInAppManager, never()).removeMessage(any(IterableInAppMessage.class));
    }

    @Test
    public void removeMessage_shouldNotCrash_whenApiIsNull() {
        // Arrange
        InAppTrackingService nullApiService = new InAppTrackingService(null);

        // Act & Assert - should not throw exception
        nullApiService.removeMessage(mockMessage);
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
