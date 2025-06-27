package com.iterable.iterableapi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build

import androidx.annotation.NonNull
import androidx.annotation.RequiresApi

import java.util.ArrayList

internal class IterableNetworkConnectivityManager private constructor(context: Context?) {
    
    companion object {
        private const val TAG = "NetworkConnectivityManager"
        
        private var sharedInstance: IterableNetworkConnectivityManager? = null
        
        @JvmStatic
        fun sharedInstance(context: Context): IterableNetworkConnectivityManager {
            if (sharedInstance == null) {
                sharedInstance = IterableNetworkConnectivityManager(context)
            }
            return sharedInstance!!
        }
    }

    private var isConnected: Boolean = false
    private val networkMonitorListeners = ArrayList<IterableNetworkMonitorListener>()

    interface IterableNetworkMonitorListener {
        fun onNetworkConnected()
        fun onNetworkDisconnected()
    }

    init {
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startNetworkCallback(context)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun startNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val networkBuilder = NetworkRequest.Builder()

        connectivityManager?.let {
            try {
                it.registerNetworkCallback(networkBuilder.build(), object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(@NonNull network: Network) {
                        super.onAvailable(network)
                        IterableLogger.v(TAG, "Network Connected")
                        isConnected = true
                        val networkListenersCopy = ArrayList(networkMonitorListeners)
                        for (listener in networkListenersCopy) {
                            listener.onNetworkConnected()
                        }
                    }

                    override fun onLost(@NonNull network: Network) {
                        super.onLost(network)
                        IterableLogger.v(TAG, "Network Disconnected")
                        isConnected = false
                        val networkListenersCopy = ArrayList(networkMonitorListeners)
                        for (listener in networkListenersCopy) {
                            listener.onNetworkDisconnected()
                        }
                    }
                })
            } catch (e: SecurityException) {
                // This security exception seems to be affecting few devices.
                // More information here: https://issuetracker.google.com/issues/175055271?pli=1
                IterableLogger.e(TAG, e.localizedMessage)
            }
        }
    }

    @Synchronized
    fun addNetworkListener(listener: IterableNetworkMonitorListener) {
        networkMonitorListeners.add(listener)
    }

    @Synchronized
    fun removeNetworkListener(listener: IterableNetworkMonitorListener) {
        networkMonitorListeners.remove(listener)
    }

    fun isConnected(): Boolean {
        return isConnected
    }
}
