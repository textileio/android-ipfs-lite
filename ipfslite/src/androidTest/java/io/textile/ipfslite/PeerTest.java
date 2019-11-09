package io.textile.ipfslite;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.io.FileUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Textile tests.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PeerTest {

    static String COMMON_CID = "QmWATWQ7fVPP2EFGu71UkfnqhYXDYH566qy47CnJDgvs8u";
    static String HELLO_WORLD_CID = "bafybeic35nent64fowmiohupnwnkfm2uxh6vpnyjlt3selcodjipfrokgi";
    static String HELLO_WORLD = "Hello World";
    static String TEST1_CID = "bafybeifi4myu2s6rkegzeb2qk6znfg76lt4gpqe6sftozg3rjy6a5cw4qa";
    static String REPO_NAME = "ipfslite";

    static Peer litePeer;

    String createRepo(Boolean reset) throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        final File filesDir = ctx.getFilesDir();
        final String path = new File(filesDir, REPO_NAME).getAbsolutePath();
        // Wipe repo
        File repo = new File(path);
        if (repo.exists() && reset == true) {
            FileUtils.deleteDirectory(repo);
        }
        return path;
    }

    void startPeer() throws Exception {
        // Initialize & start
        litePeer = new Peer(createRepo(true), BuildConfig.DEBUG);
        litePeer.start();
    }

    @Test
    public void startTest() throws Exception {
        startPeer();
        assertEquals(true, litePeer.started());
    }

    @Test
    public void GetCID() throws Exception {
        if (litePeer == null) {
            startPeer();
            assertEquals(true, litePeer.started());
        }

        byte[] file = litePeer.getFile(COMMON_CID);

        assertNotNull(file);
    }

    @Test
    public void AddFile() throws Exception {
        if (litePeer == null) {
            startPeer();
            assertEquals(true, litePeer.started());
        }
        String cid = litePeer.addFile(HELLO_WORLD.getBytes());
        assertEquals(HELLO_WORLD_CID, cid);

        byte[] res = litePeer.getFile(HELLO_WORLD_CID);
        assertEquals(HELLO_WORLD, new String(res, "UTF-8"));
    }

    @Test
    public void GetFileAsync() throws Exception {
        if (litePeer == null) {
            startPeer();
            assertEquals(true, litePeer.started());
        }
        AtomicBoolean ready = new AtomicBoolean();
        ready.getAndSet(false);
        litePeer.getFileAsync(
                HELLO_WORLD_CID, new Peer.FileHandler() {
                    @Override
                    public void onNext(byte[] data) {
                        String value = data.toString();
                        assertEquals(HELLO_WORLD, value);
                        ready.getAndSet(true);
                    }

                    @Override
                    public void onError(Throwable t) {
                        assertNull(t);
                        ready.getAndSet(true);
                    }

                    @Override
                    public void onComplete() {}
                });
        await().atMost(30, TimeUnit.SECONDS).untilTrue(ready);
    }

    @Test
    public void AddThenGetImage() throws Exception {
        if (litePeer == null) {
            startPeer();
            assertEquals(true, litePeer.started());
        }

        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File input1 = PeerTest.getCacheFile(ctx, "TEST1.JPG");

        byte[] fileBytes = Files.readAllBytes(input1.toPath());
        String cid = litePeer.addFile(fileBytes);
        assertEquals(TEST1_CID, cid);

        byte[] res = litePeer.getFile(TEST1_CID);
        assertArrayEquals(fileBytes, res);
    }

    private static File getCacheFile(Context context, String filename) throws IOException {
        File file = new File(context.getCacheDir(), filename);
        InputStream inputStream = context.getAssets().open(filename);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            } finally {
                outputStream.close();
            }
        }
        return file;
    }
}
