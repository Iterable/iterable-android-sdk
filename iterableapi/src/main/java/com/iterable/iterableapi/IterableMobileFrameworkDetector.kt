package com.iterable.iterableapi

import android.content.Context
import android.content.pm.PackageManager

object IterableMobileFrameworkDetector {
    private const val TAG = "FrameworkDetector"
    private lateinit var context: Context

    @Volatile
    private var cachedFrameworkType: IterableAPIMobileFrameworkType? = null

    private var hasClass: (String) -> Boolean = { className ->
        try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun initialize(context: Context) {
        if (context.applicationContext != null) {
            this.context = context.applicationContext
        } else {
            this.context = context
        }
        if (cachedFrameworkType == null) {
            cachedFrameworkType = detectFrameworkInternal(context)
        }
    }

    @JvmStatic
    fun detectFramework(context: Context): IterableAPIMobileFrameworkType {
        return cachedFrameworkType ?: synchronized(this) {
            cachedFrameworkType ?: detectFrameworkInternal(context).also { 
                cachedFrameworkType = it 
            }
        }
    }

    fun frameworkType(): IterableAPIMobileFrameworkType {
        return cachedFrameworkType ?: detectFramework(context)
    }

    private fun detectFrameworkInternal(context: Context): IterableAPIMobileFrameworkType {
        val hasFlutter = hasFrameworkClasses(FrameworkClasses.flutter)
        val hasReactNative = hasFrameworkClasses(FrameworkClasses.reactNative)

        return when {
            hasFlutter && hasReactNative -> {
                IterableLogger.d(TAG, "Both Flutter and React Native frameworks detected. This is unexpected.")
                when {
                    context.packageName.endsWith(".flutter") -> IterableAPIMobileFrameworkType.FLUTTER
                    hasManifestMetadata(context, ManifestMetadata.flutter) -> IterableAPIMobileFrameworkType.FLUTTER
                    hasManifestMetadata(context, ManifestMetadata.reactNative) -> IterableAPIMobileFrameworkType.REACT_NATIVE
                    else -> IterableAPIMobileFrameworkType.REACT_NATIVE
                }
            }
            hasFlutter -> IterableAPIMobileFrameworkType.FLUTTER
            hasReactNative -> IterableAPIMobileFrameworkType.REACT_NATIVE
            else -> {
                when {
                    hasManifestMetadata(context, ManifestMetadata.flutter) -> IterableAPIMobileFrameworkType.FLUTTER
                    hasManifestMetadata(context, ManifestMetadata.reactNative) -> IterableAPIMobileFrameworkType.REACT_NATIVE
                    else -> IterableAPIMobileFrameworkType.NATIVE
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
        val flutter = listOf(
            "flutterEmbedding",
            "io.flutter.embedding.android.NormalTheme",
            "io.flutter.embedding.android.SplashScreenDrawable"
        )
        
        val reactNative = listOf(
            "react_native_version",
            "expo.modules.updates.ENABLED",
            "com.facebook.react.selected.ReactActivity"
        )
    }

    private fun hasFrameworkClasses(classNames: List<String>): Boolean {
        return classNames.any { hasClass(it) }
    }

    private fun hasManifestMetadata(context: Context, metadataKeys: List<String>): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val metadata = packageInfo.applicationInfo.metaData
            metadataKeys.any { key -> metadata?.containsKey(key) == true }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error checking manifest metadata: ${e.message}")
            false
        }
    }
}