package com.iterable.iterableapi

import android.content.Context
import android.content.pm.PackageManager

object IterableMobileFrameworkDetector {
    private const val TAG = "IterableMobileFrameworkDetector"

    private object FrameworkClasses {
        val FLUTTER = arrayOf(
            "io.flutter.embedding.android.FlutterActivity",
            "io.flutter.embedding.engine.FlutterEngine",
            "io.flutter.plugin.common.MethodChannel",
            "io.flutter.app.FlutterApplication"
        )

        val REACT_NATIVE = arrayOf(
            "com.facebook.react.ReactActivity",
            "com.facebook.react.ReactApplication",
            "com.facebook.react.bridge.ReactContext",
            "com.facebook.react.ReactRootView",
            "com.facebook.react.bridge.ReactApplicationContext"
        )
    }

    private var cachedFrameworkType: IterableMobileFrameworkType? = null

    fun detectFramework(context: Context): IterableMobileFrameworkType {
        cachedFrameworkType?.let { return it }

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

            // First check: Framework classes
            val hasFlutter = hasFrameworkClasses(FrameworkClasses.FLUTTER)
            val hasReactNative = hasFrameworkClasses(FrameworkClasses.REACT_NATIVE)

            when {
                hasFlutter && hasReactNative -> {
                    IterableLogger.e(TAG, "Both Flutter and React Native frameworks detected. This is unexpected.")
                    // Resolve ambiguity using package name and metadata
                    if (context.packageName.endsWith(".flutter") || 
                        appInfo.metaData?.containsKey("flutterEmbedding") == true) {
                        IterableMobileFrameworkType.FLUTTER
                    } else {
                        IterableMobileFrameworkType.REACT_NATIVE
                    }
                }
                hasFlutter -> IterableMobileFrameworkType.FLUTTER
                hasReactNative -> IterableMobileFrameworkType.REACT_NATIVE
                else -> {
                    // Second check: Manifest metadata
                    when {
                        appInfo.metaData?.containsKey("flutterEmbedding") == true -> 
                            IterableMobileFrameworkType.FLUTTER
                        appInfo.metaData?.containsKey("react_native_version") == true -> 
                            IterableMobileFrameworkType.REACT_NATIVE
                        else -> IterableMobileFrameworkType.NATIVE
                    }
                }
            }.also { cachedFrameworkType = it }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error detecting framework type", e)
            IterableMobileFrameworkType.NATIVE
        }
    }

    private fun hasFrameworkClasses(classNames: Array<String>): Boolean {
        return classNames.any { className ->
            try {
                Class.forName(className)
                true
            } catch (ignored: ClassNotFoundException) {
                false
            }
        }
    }
} 