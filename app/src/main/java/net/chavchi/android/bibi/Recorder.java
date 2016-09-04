package net.chavchi.android.bibi;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Recorder extends AppCompatActivity {
    private BImageView imageViewer = null;

    //
    // activity life cycle
    //
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BOut.trace("Master::onCreate");

        // create user interface
        setContentView(R.layout.activity_recorder);
        // create a toolbar
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_recorder));
        // enable the back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageViewer = new BImageView(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.master_camera_preview);
        preview.addView(imageViewer);
    }

    @Override
    public void onResume() {
        super.onResume();
        BOut.trace("Master::onResume");

        ((Bibi)getApplicationContext()).recorder.setImageView(imageViewer);
        ((Bibi)getApplicationContext()).recorder.setStatusTextView((TextView)findViewById(R.id.textView_recorder_status));
    }

    @Override
    public void onPause() {
        super.onPause();
        BOut.trace("Master::onPause");

        ((Bibi)getApplicationContext()).recorder.setImageView(null);
        ((Bibi)getApplicationContext()).recorder.setStatusTextView(null);
    }

/*
    @Override
    public void onRestart() {
        super.onRestart();
        BOut.trace("Master::onRestart");
    }

    @Override
    public void onStart() {
        super.onStart();
        BOut.trace("Master::onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        BOut.trace("Master::onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BOut.trace("Master::onDestroy");
    }
*/

    //
    // menu items handling
    //
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //
    // back button press
    //
    @Override
    public void onBackPressed() {
        Bibi.getInstance().askToExit(this, "Really close?\n\nThis will disconnect all conncted viewers!");
    }
}
