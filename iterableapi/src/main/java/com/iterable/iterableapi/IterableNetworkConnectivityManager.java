package com.iterable.iterableapi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class IterableNetworkConnectivityManager {
    private static final String TAG = "NetworkConnectivityManager";
    private boolean isConnected = true;

    private static IterableNetworkConnectivityManager sharedInstance;

    private ArrayList<IterableNetworkMonitorListener> networkMonitorListeners = new ArrayList<>();
    private Set<Network> networkSet = new HashSet<>();

    public interface IterableNetworkMonitorListener {
        void onNetworkConnected();

        void onNetworkDisconnected();
    }

    static IterableNetworkConnectivityManager sharedInstance(Context context) {
        if (sharedInstance == null) {
            sharedInstance = new IterableNetworkConnectivityManager(context);
        }
        return sharedInstance;
    }

    private IterableNetworkConnectivityManager(Context context) {
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startNetworkCallback(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startNetworkCallback(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isConnected = networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        if (connectivityManager != null) {
            try {
                connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        networkSet.add(network);
                        IterableLogger.v(TAG, "Network Connected");
                        isConnected = true;
                        ArrayList<IterableNetworkMonitorListener> networkListenersCopy = new ArrayList<>(networkMonitorListeners);
                        for (IterableNetworkMonitorListener listener : networkListenersCopy) {
                            listener.onNetworkConnected();
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        IterableLogger.v(TAG, "Network Disconnected");
                        networkSet.remove(network);
                        if (networkSet.isEmpty()) {
                            isConnected = false;
                            ArrayList<IterableNetworkMonitorListener> networkListenersCopy = new ArrayList<>(networkMonitorListeners);
                            for (IterableNetworkMonitorListener listener : networkListenersCopy) {
                                listener.onNetworkDisconnected();
                            }
                        }
                    }
                });
            } catch (SecurityException e) {
                // This security exception seems to be affecting few devices.
                // More information here: https://issuetracker.google.com/issues/175055271?pli=1
                IterableLogger.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    synchronized void addNetworkListener(IterableNetworkMonitorListener listener) {
        networkMonitorListeners.add(listener);
    }

    synchronized void removeNetworkListener(IterableNetworkMonitorListener listener) {
        networkMonitorListeners.remove(listener);
    }

    boolean isConnected() {
        return isConnected;
    }
}
