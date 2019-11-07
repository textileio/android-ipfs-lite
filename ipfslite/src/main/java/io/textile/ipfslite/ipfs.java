package io.textile.ipfslite;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.HashSet;

import mobile.Mobile;

/**
 * Provides top level access to the Textile API
 */
public class ipfs implements LifecycleObserver {

    private static final String TAG = "ipfs";
    private Context applicationContext;

    enum NodeState {
        Start, Stop
    }

    public static NodeState state = NodeState.Stop;
    public static String path;

    /**
     * init the gRPC IPFS Lite server instance with the provided host string
     * @throws Exception The exception that occurred
     */
    public static void initialize(String datastorePath) throws Exception {
        if (state == NodeState.Start) {
            stop();
        }
        path = datastorePath;
    }

    public static void start() throws Exception {
        if (state == NodeState.Start) {
            return;
        }
        Mobile.start(path);
        state = NodeState.Start;
    }

    public static void stop() {
        if (state == NodeState.Stop) {
            return;
        }
        Mobile.stop();
        state = NodeState.Stop;
    }


//    @OnLifecycleEvent(Lifecycle.Event.ON_START)
//    void onForeground() {
//        try {
//            start();
//        } catch (Exception e) {
//            // log error maybe
//        }
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
//    void onBackground() {
////         stop();
//        // Test for now without stopping.
//    }
}