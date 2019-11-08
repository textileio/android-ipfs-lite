package io.textile.ipfslite;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.protobuf.ByteString;

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
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * Textile tests.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PeerTest {

    static String REPO_NAME = "ipfslite";
    static String COMMON_CID = "bafybeic35nent64fowmiohupnwnkfm2uxh6vpnyjlt3selcodjipfrokgi";
    static String TEST0_CID = "bafybeibvgyphgiv2paoizwtfxdbxevumzilvvkfjt7bdqqiet27wyy6jsi";
    static String HELLO_WORLD = "Hello World";
    static Peer litePeer;

    String resetRepo() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        final File filesDir = ctx.getFilesDir();
        final String path = new File(filesDir, REPO_NAME).getAbsolutePath();
        // Wipe repo
        File repo = new File(path);
        if (repo.exists()) {
            FileUtils.deleteDirectory(repo);
        }
        return path;
    }

    void startPeer() throws Exception {
        // Initialize
        litePeer = new Peer(resetRepo());
        // Start
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
        }

        assertEquals(true, litePeer.started());

        byte[] file = litePeer.getFile(COMMON_CID);

        assertNotNull(file);
    }

    @Test
    public void AddFile() throws Exception {
        if (litePeer == null) {
            startPeer();
        }

        assertEquals(true, litePeer.started());

        String cid = litePeer.addFile(HELLO_WORLD.getBytes());
        assertEquals(COMMON_CID, cid);

        byte[] res = litePeer.getFile(COMMON_CID);
        assertEquals(HELLO_WORLD, new String(res, "UTF-8"));

    }

    @Test
    public void AddLargeFile() throws Exception {
        if (litePeer == null) {
            startPeer();
        }

        assertEquals(true, litePeer.started());


        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // TODO change this to TEST1 after max size increasted on grpc server
        File input1 = PeerTest.getCacheFile(ctx, "TEST0.JPG");

        byte[] fileBytes = Files.readAllBytes(input1.toPath());
        String cid = litePeer.addFile(fileBytes);
        assertEquals(TEST0_CID, cid);

        byte[] res = litePeer.getFile(TEST0_CID);
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
