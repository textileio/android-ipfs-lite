package io.textile.textileexample;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.File;

import io.textile.ipfslite.Peer;

public class MainActivity extends AppCompatActivity {

    Peer litePeer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initIPFS();
    }

    public void onButtonClick(View v) {
        try {
            String file = litePeer.getFile("QmY7Yh4UquoXHLPFo2XbhXkhBvFoPwmQUSa92pxnxjQuPU");
            System.out.println(file);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void initIPFS() {
        try {
            Context ctx = getApplicationContext();

            final File filesDir = ctx.getFilesDir();
//            final String path = filesDir.getAbsolutePath();
            final String path = new File(filesDir, "ipfslite").getAbsolutePath();
            litePeer = new Peer(path);
            litePeer.start();
            System.out.println("Started ?");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

//    private void destroyTextile() {
//        Textile.instance().destroy();
//    }
}
