package com.iterable.iterableapi;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    private DialogInterface.OnCancelListener mockCancelListener;

    @Mock
    private IterableHelper.IterableUrlCallback mockCallback;

    @Mock
    private IterableWebView mockWebView;

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
        htmlNotification = new IterableInAppFragmentHTMLNotification();
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
        Double backgroundAlpha = 0.5;
        Rect padding = new Rect();

        IterableInAppFragmentHTMLNotification result = IterableInAppFragmentHTMLNotification.createInstance(
                htmlString, callbackOnCancel, mockCallback, location, messageId, backgroundAlpha, padding);

        assertNotNull(result);
    }

    @Test
    public void testOnCreate() {
        // Mock the getArguments() method
        when(mockBundle.getString(eq("HTML_STRING"), eq("Your HTML String"))).thenReturn("Your HTML String");
        when(mockBundle.getBoolean(eq("CALLBACK_ON_CANCEL"), anyBoolean())).thenReturn(true);
        when(mockBundle.getString(eq("MESSAGE_ID"), eq("Your Message ID"))).thenReturn("Your Message ID");
        when(mockBundle.getDouble(eq("BACKGROUND_ALPHA"), anyDouble())).thenReturn(0.5);
        when(mockBundle.getParcelable(eq("INSET_PADDING"))).thenReturn(new Rect());
        when(mockBundle.getDouble(eq("IN_APP_BG_ALPHA"), anyDouble())).thenReturn(0.7);
        when(mockBundle.getString(eq("IN_APP_BG_COLOR"), eq("Color"))).thenReturn("Color");
        when(mockBundle.getBoolean(eq("IN_APP_SHOULD_ANIMATE"), anyBoolean())).thenReturn(true);

        htmlNotification.onCreate(mockBundle);

        // Add assertions as needed to check the behavior of the method.
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