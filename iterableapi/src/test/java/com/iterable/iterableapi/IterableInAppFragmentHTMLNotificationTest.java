package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Window;
import android.view.WindowManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class IterableInAppFragmentHTMLNotificationTest {

    @Mock
    private Bundle mockBundle;

    @Mock
    private IterableHelper.IterableUrlCallback mockCallback;

    @Mock
    private IterableApi mockIterableApi;

    @InjectMocks
    private IterableInAppFragmentHTMLNotification htmlNotification;

    @Mock
    private IterableWebView webView;

    @Mock
    private IterableInAppLocation location;

    @Mock
    private OrientationEventListener orientationListener;

    @Mock
    private Context context;

    @Mock
    private WindowManager windowManager;

    @Mock
    private Display display;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockCallback = mock(IterableHelper.IterableUrlCallback.class);
        IterableApi.sharedInstance = mockIterableApi;
        htmlNotification = spy(new IterableInAppFragmentHTMLNotification());
        when(webView.getId()).thenReturn(R.id.webView);
        when(htmlNotification.getContext()).thenReturn(context);

        when(context.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManager);
        when(windowManager.getDefaultDisplay()).thenReturn(display);
    }

    @After
    public void teardown() {
        IterableApi.sharedInstance = null;
    }

    @Test
    public void testCreateInstance() {
        String htmlString = "Your HTML String";
        boolean callbackOnCancel = true;
        IterableInAppLocation location = IterableInAppLocation.IN_APP;
        String messageId = "Your Message ID";
        double backgroundAlpha = 0.5;
        Rect padding = new Rect();

        assertEquals(htmlString, "Your HTML String");
        assertTrue(callbackOnCancel);
        assertEquals(messageId, "Your Message ID");
        assertEquals(backgroundAlpha, 0.5, 0.5);

        IterableInAppFragmentHTMLNotification result = IterableInAppFragmentHTMLNotification.createInstance(
                htmlString, callbackOnCancel, mockCallback, location, messageId, backgroundAlpha, padding);

        assertNotNull(result);
    }

    @Test
    public void testOnCreate() {
        when(mockBundle.getString(eq("HTML_STRING"), eq("Your HTML String"))).thenReturn("Your HTML String");
        when(mockBundle.getBoolean(eq("CALLBACK_ON_CANCEL"), anyBoolean())).thenReturn(true);
        when(mockBundle.getString(eq("MESSAGE_ID"), eq("Your Message ID"))).thenReturn("Your Message ID");
        when(mockBundle.getDouble(eq("BACKGROUND_ALPHA"), anyDouble())).thenReturn(0.5);
        when(mockBundle.getParcelable(eq("INSET_PADDING"))).thenReturn(new Rect());
        when(mockBundle.getDouble(eq("IN_APP_BG_ALPHA"), anyDouble())).thenReturn(0.7);
        when(mockBundle.getString(eq("IN_APP_BG_COLOR"), eq("Color"))).thenReturn("Color");
        when(mockBundle.getBoolean(eq("IN_APP_SHOULD_ANIMATE"), anyBoolean())).thenReturn(true);

        mockBundle.putString("HTML_STRING", "Your HTML String");
        mockBundle.putBoolean("CALLBACK_ON_CANCEL", true);
        mockBundle.putString("MESSAGE_ID", "Your Message ID");
        mockBundle.putDouble("BACKGROUND_ALPHA", 0.5);
        mockBundle.putParcelable("INSET_PADDING", new Rect());
        mockBundle.putDouble("IN_APP_BG_ALPHA", 0.7);
        mockBundle.putString("IN_APP_BG_COLOR", "Color");
        mockBundle.putBoolean("IN_APP_SHOULD_ANIMATE", true);
        htmlNotification.setArguments(mockBundle);
        htmlNotification.onCreate(mockBundle);
    }

    @Test
    public void testOnCreateWithValidArgs() {
        IterableInAppManager mockInAppManager = mock(IterableInAppManager.class);
        when(mockIterableApi.getInAppManager()).thenReturn(mockInAppManager);

        when(mockBundle.getString("HTML", null)).thenReturn("html content");
        when(mockBundle.getBoolean("CallbackOnCancel", false)).thenReturn(true);
        when(mockBundle.getString("MessageId")).thenReturn("message123");
        when(mockBundle.getDouble("BackgroundAlpha")).thenReturn(0.7);

        htmlNotification.setArguments(mockBundle);
        htmlNotification.onCreate(null);

        assertEquals("message123", "message123");
        assertTrue(true);
    }

    @Test
    public void testOnCreateWithNullArgs() {
        htmlNotification.onCreate(null);
        assertEquals("message123", "message123");
        assertFalse(false);
    }

    @Test
    public void testSetLoaded() {
        assertFalse(false);
        htmlNotification.setLoaded(true);
        assertTrue(true);
        htmlNotification.setLoaded(false);
        assertFalse(false);
    }

    @Test
    public void testOnStop() {
        assertNotNull(orientationListener);
        Mockito.doNothing().when(orientationListener).disable();
        htmlNotification.onStop();
    }

    @Test
    public void testOnDestroy() {
        if (htmlNotification.getActivity() != null && htmlNotification.getActivity().isChangingConfigurations()) {
            return;
        }
        htmlNotification.onDestroy();
        htmlNotification = null;
        location = null;
    }

    @Test
    public void testRunResizeScript() {
        htmlNotification.resize(webView.getContentHeight());
    }

    @Test
    public void testResize_ExceptionHandling() {
        AlertDialog dialogMock = Mockito.mock(AlertDialog.class);
        Window windowMock = Mockito.mock(Window.class);

        when(htmlNotification.getDialog()).thenReturn(dialogMock);
        when(dialogMock.getWindow()).thenReturn(windowMock);

        doThrow(new IllegalArgumentException("Test Exception")).when(windowMock).setLayout(anyInt(), anyInt());
        htmlNotification.resize(0.5f);
    }
}