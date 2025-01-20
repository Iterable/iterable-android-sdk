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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    }

    @Test
    fun `test native detection when no framework classes or metadata present`() {
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.NATIVE, result)
    }

    @Test
    fun `test flutter detection through metadata`() {
        mockApplicationInfo.metaData.putBoolean("flutterEmbedding", true)
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `test react native detection through metadata`() {
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.REACT_NATIVE, result)
    }

    @Test
    fun `test flutter detection through package name`() {
        `when`(mockContext.packageName).thenReturn("com.example.flutter.app")
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `test framework detection caching`() {
        // First call should detect native
        val result1 = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.NATIVE, result1)

        // Add Flutter metadata
        mockApplicationInfo.metaData.putBoolean("flutterEmbedding", true)

        // Second call should still return native due to caching
        val result2 = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.NATIVE, result2)
    }

    @Test
    fun `test error handling returns native`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            `when`(mockPackageManager.getPackageInfo(
                mockContext.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )).thenThrow(PackageManager.NameNotFoundException())
        } else {
            @Suppress("DEPRECATION")
            `when`(mockPackageManager.getPackageInfo(
                mockContext.packageName,
                PackageManager.GET_META_DATA
            )).thenThrow(PackageManager.NameNotFoundException())
        }
        
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.NATIVE, result)
    }

    @Test
    fun `test ambiguity resolution prefers flutter when both detected with flutter package name`() {
        // Mock both frameworks being detected
        `when`(mockContext.packageName).thenReturn("com.example.flutter.app")
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `test ambiguity resolution prefers flutter when both detected with flutter metadata`() {
        // Mock both frameworks being detected
        mockApplicationInfo.metaData.putBoolean("flutterEmbedding", true)
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.FLUTTER, result)
    }

    @Test
    fun `test ambiguity resolution defaults to react native when no flutter indicators`() {
        // Mock both frameworks being detected but no Flutter indicators
        mockApplicationInfo.metaData.putString("react_native_version", "0.70.0")
        
        val result = IterableMobileFrameworkDetector.detectFramework(mockContext)
        assertEquals(IterableMobileFrameworkType.REACT_NATIVE, result)
    }
} 