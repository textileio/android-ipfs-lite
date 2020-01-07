package io.textile.ipfslite;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
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

class PeerException extends Exception
{
    public PeerException(String message)
    {
        super(message);
    }
}

/**
 * Provides top level access to the Textile API
 */
public class Peer implements LifecycleObserver {

    private static final String TAG = "Peer";
    private final static Logger logger =
            Logger.getLogger(TAG);

    private static Boolean mode;
    public static String path;
    private static long port;

    private static ManagedChannel channel;
    private static IpfsLiteGrpc.IpfsLiteBlockingStub blockingStub;
    private static IpfsLiteGrpc.IpfsLiteStub asyncStub;

    enum NodeState {
        Start, Stop
    }

    public static NodeState state = NodeState.Stop;

    /**
     * init the gRPC IPFS Lite server instance with the provided repo path
     */
    public Peer(String datastorePath, Boolean debug) {
        path = datastorePath;
        mode = debug;
    }

    void ready() throws PeerException {
        if (!started()) {
            throw new PeerException("Peer not started");
        }
    }

    /**
     * start IPFS Lite instance with the provided repo path
     * @throws Exception The exception that occurred
     */
    public static void start() throws Exception {
        if (state == NodeState.Start) {
            return;
        }
        port = Mobile.start(path, mode);

        channel = ManagedChannelBuilder
                .forAddress("localhost", Math.toIntExact(port))
                .usePlaintext()
                .build();
        blockingStub = IpfsLiteGrpc.newBlockingStub(channel);
        asyncStub = IpfsLiteGrpc.newStub(channel);

        state = NodeState.Start;
    }

    public static void stop() throws Exception {
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

    void streamDataChunks(byte[] data, StreamObserver<AddFileRequest> requestStream) {
        try {
            // Start stream
            AddFileRequest.Builder requestHeader = FileRequestHeader();
            requestStream.onNext(requestHeader.build());
            // Send file segments of 1024b
            int blockSize = 1024;
            int blockCount = (data.length + blockSize - 1) / blockSize;

            byte[] range;
            for (int i = 1; i < blockCount; i++) {
                int idx = (i - 1) * blockSize;
                range = Arrays.copyOfRange(data, idx, idx + blockSize);
                AddFileRequest.Builder requestData = FileData(ByteString.copyFrom(range));
                requestStream.onNext(requestData.build());
            }
            int end = -1;
            if (data.length % blockSize == 0) {
                end = data.length;
            } else {
                end = data.length % blockSize + blockSize * (blockCount - 1);
            }
            range = Arrays.copyOfRange(data, (blockCount - 1) * blockSize, end);
            AddFileRequest.Builder requestData = FileData(ByteString.copyFrom(range));
            requestStream.onNext(requestData.build());
        } catch (RuntimeException e) {
            requestStream.onError(e);
            throw e;
        }
        requestStream.onCompleted();
    }

    public interface AddFileHandler {
        void onNext(final String cid);
        void onComplete();
        void onError(final Throwable t);
    }

    public void addFile(byte[] data, final AddFileHandler handler) {

        StreamObserver<AddFileRequest> addFileRequest = asyncStub.addFile(new StreamObserver<AddFileResponse>() {
            @Override
            public void onNext(AddFileResponse value) {
                try {
                    String cid = value.getNode().getBlock().getCid();
                    handler.onNext(cid);
                } catch (Throwable t) {
                    handler.onError(t);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "GetFileError: " + t.getLocalizedMessage());
                handler.onError(t);
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "GetFileComplete");
                handler.onComplete();
            }
        });

        // Stream the file over a background thread
        new Thread(() -> {
            streamDataChunks(data, addFileRequest);
        }).start();
    }

    public String addFileSync(byte[] data) throws Exception {
        ready();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicReference<String> CID = new AtomicReference<>("");
        StreamObserver<AddFileResponse> responseObserver = new StreamObserver<AddFileResponse>() {
            @Override
            public void onNext(AddFileResponse value) {
                String res = value.getNode().getBlock().getCid();
                logger.log(Level.INFO, "AddFile onNext: " + res);
                CID.set(res);
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "AddFile onError: " + t.getLocalizedMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "AddFile: Complete");
                finishLatch.countDown();
            }
        };
        StreamObserver<AddFileRequest> addFileRequest = asyncStub.addFile(responseObserver);

        // Stream the file over a background thread
        new Thread(() -> {
            streamDataChunks(data, addFileRequest);
        }).start();

        // this will take as long as you give it.
        finishLatch.await(30, TimeUnit.SECONDS);
        return CID.get();
    }

    public interface GetFileHandler {
        void onNext(final byte[] data);
        void onComplete();
        void onError(final Throwable t);
    }
    public void getFile(String cid, final GetFileHandler handler) {
        asyncStub.getFile(FileRequest(cid), new StreamObserver<GetFileResponse>() {
            @Override
            public void onNext(GetFileResponse value) {
                handler.onNext(value.getChunk().toByteArray());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "GetFileError: " + t.getLocalizedMessage());
                handler.onError(t);
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "GetFileComplete");
                handler.onComplete();
            }
        });
    }

    public byte[] getFileSync(String cid) throws Exception {
        ready();
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

    public interface ResolveLinkHandler {
        void onNext(final String cid);
        void onComplete();
        void onError(final Throwable t);
    }

    public void resolveLink(String link, final ResolveLinkHandler handler) {
        String[] parts = link.split("/");
        if (parts.length == 0) {
            handler.onComplete();
        }

        ResolveLinkRequest.Builder request = ResolveLinkRequest
                .newBuilder()
                .setNodeCid(parts[0]);


        String linkPath = link.replace(parts[0] + "/", "");
        if (linkPath != "") {
            request.addPath(linkPath);
        }

        asyncStub.resolveLink(request.build(), new StreamObserver<ResolveLinkResponse>() {
            @Override
            public void onNext(ResolveLinkResponse value) {
                handler.onNext(value.getLink().getCid());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "ResolveLinkResponseError: " + t.getLocalizedMessage());
                handler.onError(t);
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "ResolveLinkResponseComplete");
                handler.onComplete();
            }
        });
    }

    public interface ResolveNodeHandler {
        void onNext(final Node node);
        void onComplete();
        void onError(final Throwable t);
    }

    public void getNode(String cid, final ResolveNodeHandler handler) {
        GetNodeRequest.Builder request = GetNodeRequest
                .newBuilder()
                .setCid(cid);

        asyncStub.getNode(request.build(), new StreamObserver<GetNodeResponse>() {
            @Override
            public void onNext(GetNodeResponse value) {
                Node node = value.getNode();
                handler.onNext(node);
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "GetNodeError: " + t.getLocalizedMessage());
                handler.onError(t);
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "GetNodeComplete");
                handler.onComplete();
            }
        });
    }

    public interface RemoveNodeHandler {
        void onNext(String cid);
        void onComplete();
        void onError(final Throwable t);
    }
    public void removeNode(String cid, final RemoveNodeHandler handler) {
        RemoveNodeRequest.Builder request = RemoveNodeRequest
                .newBuilder()
                .setCid(cid);

        asyncStub.removeNode(request.build(), new StreamObserver<RemoveNodeResponse>() {
            @Override
            public void onNext(RemoveNodeResponse value) {
                handler.onNext(value.toString());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.INFO, "RemoveNodeError: " + t.getLocalizedMessage());
                handler.onError(t);
            }

            @Override
            public void onCompleted() {
                logger.log(Level.INFO, "RemoveNodeComplete");
                handler.onComplete();
            }
        });
    }

    public Boolean started() {
        return state == NodeState.Start;
    }
}