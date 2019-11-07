package io.textile.ipfslite;

import android.arch.lifecycle.LifecycleObserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Iterator;

import mobile.Mobile;
import io.textile.grpc_ipfs_lite.*;

/**
 * Provides top level access to the Textile API
 */
public class Peer implements LifecycleObserver {

    private static final String TAG = "Peer";

    private final ManagedChannel channel;
    private final IpfsLiteGrpc.IpfsLiteBlockingStub blockingStub;
    private final IpfsLiteGrpc.IpfsLiteStub asyncStub;

    enum NodeState {
        Start, Stop
    }

    public static NodeState state = NodeState.Stop;
    public static String path;

    /** Construct client for accessing RouteGuide server at {@code host:port}. */
    public Peer(String datastorePath) {
        path = datastorePath;
        channel = ManagedChannelBuilder.forAddress("localhost", 10000).usePlaintext().build();
        blockingStub = IpfsLiteGrpc.newBlockingStub(channel);
        asyncStub = IpfsLiteGrpc.newStub(channel);
    }

    /**
     * init the gRPC IPFS Lite server instance with the provided host string
     * @throws Exception The exception that occurred
     */
//    public static void initialize(String datastorePath) throws Exception {
//        if (state == NodeState.Start) {
//            stop();
//        }
//        path = datastorePath;
//        channel = ManagedChannelBuilder.forAddress("localhost", 10000).usePlaintext().build();
//        blockingStub = IpfsLiteGrpc.newBlockingStub(channel);
//        asyncStub = IpfsLiteGrpc.newStub(channel);
//    }

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
    static GetFileRequest FileRequest (String cid) {
         return GetFileRequest.newBuilder()
                 .setCid(cid)
                 .build();
    }
    public String getFile(String cid) throws Exception {
        GetFileRequest request = FileRequest(cid);
        Iterator<GetFileResponse> response = blockingStub.getFile(request);
        if (response.hasNext()) {
            return response.next().toString();
        }

        return response.toString();
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