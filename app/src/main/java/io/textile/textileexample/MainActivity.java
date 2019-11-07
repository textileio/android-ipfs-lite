package io.textile.textileexample;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.File;

import io.textile.ipfslite.ipfs;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initIPFS();
    }

    public void onButtonClick(View v) {
        ipfs.stop();
        System.out.println("Stopped ?");
        try {
            ipfs.start();
            System.out.println("Started ?");
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
            ipfs.initialize(path);
            ipfs.start();
            System.out.println("Started ?");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

//    private void destroyTextile() {
//        Textile.instance().destroy();
//    }
}
