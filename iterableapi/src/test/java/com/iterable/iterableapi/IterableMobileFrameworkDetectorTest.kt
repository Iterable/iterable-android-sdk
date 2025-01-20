package com.iterable.iterableapi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
class IterableMobileFrameworkDetectorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockPackageInfo: PackageInfo

    @Mock
    private lateinit var mockApplicationInfo: ApplicationInfo

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.packageName).thenReturn("com.test.app")
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        
        mockApplicationInfo.metaData = Bundle()
        mockPackageInfo.applicationInfo = mockApplicationInfo
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            `when`(mockPackageManager.getPackageInfo(
                mockContext.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )).thenReturn(mockPackageInfo)
        } else {
            @Suppress("DEPRECATION")
            `when`(mockPackageManager.getPackageInfo(
                mockContext.packageName,
                PackageManager.GET_META_DATA
            )).thenReturn(mockPackageInfo)
        }

        // Initialize the detector with the mock context
        IterableMobileFrameworkDetector.initialize(mockContext)

        // Use reflection to modify class detection for testing
        val hasClassField = IterableMobileFrameworkDetector::class.java.getDeclaredField("hasClass")
        hasClassField.isAccessible = true
        hasClassField.set(null) { className: String -> 
            listOf(
                "io.flutter.embedding.android.FlutterActivity",
                "com.facebook.react.ReactActivity",
                "io.flutter.view.FlutterView",
                "com.reactnativenavigation.react.ReactActivity"
            ).contains(className)
        }

        // Use reflection to modify the detectFrameworkInternal method to add logging
        val detectFrameworkMethod = IterableMobileFrameworkDetector::class.java.getDeclaredMethod(
            "detectFrameworkInternal", 
            Context::class.java
        )
        detectFrameworkMethod.isAccessible = true
    }

    @Test
    fun `detect native framework when no metadata`() {
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.NATIVE, result)
    }

    @Test
    fun `detect flutter framework through package name`() {
        `when`(mockContext.packageName).thenReturn("com.example.flutter.app")
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `detect flutter framework through metadata`() {
        mockApplicationInfo.metaData.putBoolean("flutterEmbedding", true)
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `detect react native framework through metadata`() {
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.REACT_NATIVE, result)
    }

    @Test
    fun `detect flutter when both frameworks present with flutter package name`() {
        `when`(mockContext.packageName).thenReturn("com.example.flutter.app")
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `detect flutter when both frameworks present with flutter metadata`() {
        mockApplicationInfo.metaData.putBoolean("flutterEmbedding", true)
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }
} 