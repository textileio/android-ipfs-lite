package io.textile.ipfslite;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.arch.lifecycle.LifecycleObserver;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// This is the gRPC IPFS Lite gomobile framework output
import io.grpc.stub.StreamObserver;
import mobile.Mobile;

// This is the proto generated services and methods
import io.textile.grpc_ipfs_lite.*;

/**
 * Provides top level access to the Textile API
 */
public class Peer implements LifecycleObserver {

    private static final String TAG = "Peer";

    private final ManagedChannel channel;
    private final IpfsLiteGrpc.IpfsLiteBlockingStub blockingStub;
    private final IpfsLiteGrpc.IpfsLiteStub asyncStub;

    private final static Logger logger =
            Logger.getLogger(TAG);

    enum NodeState {
        Start, Stop
    }

    public static NodeState state = NodeState.Stop;
    public static String path;

    /**
     * init the gRPC IPFS Lite server instance with the provided repo path
     */
    public Peer(String datastorePath) {
        path = datastorePath;
        //channel = OkHttpChannelBuilder
        //        .forAddress("localhost", 10000)
        //        .maxInboundMessageSize(998430000)
        //        .maxInboundMetadataSize(998430000)
        //        .usePlaintext()
        //        .build();
        channel = ManagedChannelBuilder
                .forAddress("localhost", 10000)
                .maxInboundMessageSize(9984308)
                .usePlaintext()
                .build();
        blockingStub = IpfsLiteGrpc.newBlockingStub(channel);
        asyncStub = IpfsLiteGrpc.newStub(channel);
    }

    /**
     * start IPFS Lite instance with the provided repo path
     * @throws Exception The exception that occurred
     */
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

    public byte[] getFile(String cid) throws Exception {
        GetFileRequest request = FileRequest(cid);
        Iterator<GetFileResponse> response = blockingStub.getFile(request);
        // TODO is there a more efficient way to do this?
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (response.hasNext()) {
            byte[] bytes = response.next().getChunk().toByteArray();
            baos.write(bytes);
        }
        return baos.toByteArray();
    }


    static AddParams.Builder AddFileParams (ByteString data) {
        return AddParams.newBuilder()
                .setChunker(data.toStringUtf8());
    }
    static AddFileRequest.Builder FileData (ByteString data) {
        return AddFileRequest.newBuilder()
                .setAddParams(AddFileParams(data))
                .setChunk(data);
    }
    static AddFileRequest.Builder FileRequestHeader () {
        AddParams.Builder params = AddParams.newBuilder();
        return AddFileRequest.newBuilder()
                .setAddParams(params);
    }
    public String addFile(byte[] data) throws Exception {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicReference<String> CID = new AtomicReference<>("");
        StreamObserver<AddFileResponse> responseObserver = new StreamObserver<AddFileResponse>() {
            @Override
            public void onNext(AddFileResponse value) {
                String res = value.getNode().getBlock().getCid();
                logger.log(Level.INFO, "AddFile: " + res);
                CID.set(res);
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "AddFile: " + t.getLocalizedMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "AddFile: Complete");
                finishLatch.countDown();
            }
        };
        StreamObserver<AddFileRequest> requestObserver = asyncStub.addFile(responseObserver);

        try {
            // Start stream
            AddFileRequest.Builder requestHeader = FileRequestHeader();
            requestObserver.onNext(requestHeader.build());
            // Send file segments of 1024b
            int blockSize = 1024;
            int blockCount = (data.length + blockSize - 1) / blockSize;

            byte[] range = null;
            for (int i = 1; i < blockCount; i++) {
                int idx = (i - 1) * blockSize;
                range = Arrays.copyOfRange(data, idx, idx + blockSize);
                AddFileRequest.Builder requestData = FileData(ByteString.copyFrom(range));
                requestObserver.onNext(requestData.build());
            }
            int end = -1;
            if (data.length % blockSize == 0) {
                end = data.length;
            } else {
                end = data.length % blockSize + blockSize * (blockCount - 1);
            }
            range = Arrays.copyOfRange(data, (blockCount - 1) * blockSize, end);
            AddFileRequest.Builder requestData = FileData(ByteString.copyFrom(range));
            requestObserver.onNext(requestData.build());

//            ByteString byteString = ByteString.copyFrom(data);
//            AddFileRequest.Builder requestData = FileData(ByteString.copyFrom(data));
//            requestObserver.onNext(requestData.build());
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();
        // this will take as long as you give it.
        finishLatch.await(30, TimeUnit.SECONDS);
        return CID.get();
    }

    public Boolean started() {
        return state == NodeState.Start;
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