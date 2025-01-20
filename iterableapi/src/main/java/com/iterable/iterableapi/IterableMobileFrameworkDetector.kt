package com.iterable.iterableapi

import android.content.Context
import android.content.pm.PackageManager

object IterableMobileFrameworkDetector {
    private const val TAG = "FrameworkDetector"
    private lateinit var context: Context

    // Thread-safe cached framework type
    @Volatile
    private var cachedFrameworkType: IterableMobileFrameworkType? = null

    fun initialize(context: Context) {
        if (context.applicationContext != null) {
            this.context = context.applicationContext
        } else {
            this.context = context
        }
        // Initialize cache on first initialization
        if (cachedFrameworkType == null) {
            cachedFrameworkType = detectFrameworkInternal(context)
        }
    }

    // Static detection method with caching
    @JvmStatic
    fun detectFramework(context: Context): IterableMobileFrameworkType {
        return cachedFrameworkType ?: synchronized(this) {
            cachedFrameworkType ?: detectFrameworkInternal(context).also { 
                cachedFrameworkType = it 
            }
        }
    }

    // For backward compatibility - uses initialized context
    fun frameworkType(): IterableMobileFrameworkType {
        return cachedFrameworkType ?: detectFramework(context)
    }

    // Internal detection logic
    private fun detectFrameworkInternal(context: Context): IterableMobileFrameworkType {
        val hasFlutter = hasFrameworkClasses(FrameworkClasses.flutter)
        val hasReactNative = hasFrameworkClasses(FrameworkClasses.reactNative)

        return when {
            hasFlutter && hasReactNative -> {
                println("$TAG: Both Flutter and React Native frameworks detected. This is unexpected.")
                // Check multiple indicators for Flutter
                when {
                    context.packageName.endsWith(".flutter") -> IterableMobileFrameworkType.FLUTTER
                    hasManifestMetadata(context, ManifestMetadata.flutter) -> IterableMobileFrameworkType.FLUTTER
                    hasManifestMetadata(context, ManifestMetadata.reactNative) -> IterableMobileFrameworkType.REACT_NATIVE
                    else -> IterableMobileFrameworkType.REACT_NATIVE
                }
            }
            hasFlutter -> IterableMobileFrameworkType.FLUTTER
            hasReactNative -> IterableMobileFrameworkType.REACT_NATIVE
            else -> {
                // Check manifest metadata as fallback
                when {
                    hasManifestMetadata(context, ManifestMetadata.flutter) -> IterableMobileFrameworkType.FLUTTER
                    hasManifestMetadata(context, ManifestMetadata.reactNative) -> IterableMobileFrameworkType.REACT_NATIVE
                    else -> IterableMobileFrameworkType.NATIVE
                }
            }
        }
    }

    private object FrameworkClasses {
        val flutter = listOf(
            "io.flutter.embedding.engine.FlutterEngine",
            "io.flutter.plugin.common.MethodChannel",
            "io.flutter.embedding.android.FlutterActivity",
            "io.flutter.embedding.android.FlutterFragment"
        )

        val reactNative = listOf(
            "com.facebook.react.ReactRootView",
            "com.facebook.react.bridge.ReactApplicationContext",
            "com.facebook.react.ReactActivity",
            "com.facebook.react.ReactApplication",
            "com.facebook.react.bridge.ReactContext"
        )
    }

    private object ManifestMetadata {
        // Flutter metadata keys
        val flutter = listOf(
            "flutterEmbedding",
            "io.flutter.embedding.android.NormalTheme",
            "io.flutter.embedding.android.SplashScreenDrawable"
        )
        
        // React Native metadata keys
        val reactNative = listOf(
            "react_native_version",
            "expo.modules.updates.ENABLED",
            "com.facebook.react.selected.ReactActivity"
        )
    }

    private fun hasFrameworkClasses(classNames: List<String>): Boolean {
        return classNames.any { hasClass(it) }
    }

    private fun hasClass(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun hasManifestMetadata(context: Context, metadataKeys: List<String>): Boolean {
        return try {
            // Using packageManager.getPackageInfo instead of getApplicationInfo
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val metadata = packageInfo.applicationInfo.metaData
            metadataKeys.any { key -> metadata?.containsKey(key) == true }
        } catch (e: Exception) {
            println("$TAG: Error checking manifest metadata: ${e.message}")
            false
        }
    }
}