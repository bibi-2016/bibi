package net.chavchi.android.bibi;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

public class Monitor extends AppCompatActivity {
    private BImageView imageViewer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BOut.trace("Monitor::onCreate");

        // create user interface
        setContentView(R.layout.activity_monitor);
        // create a toolbar
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_monitor));
        // enable the back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageViewer = new BImageView(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.monitor_camera_preview);
        preview.addView(imageViewer);
    }

    @Override
    public void onResume() {
        super.onResume();
        BOut.trace("Monitor::onResume");

        ((Bibi)getApplicationContext()).viewer.setImageView(imageViewer);
        ((Bibi)getApplicationContext()).setMonitor(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BOut.trace("Monitor::onPause");

        ((Bibi)getApplicationContext()).viewer.setImageView(null);
        ((Bibi)getApplicationContext()).setMonitor(null);
    }

/*
    @Override
    public void onRestart() {
        super.onRestart();
        BOut.trace("Monitor::onRestart");
    }

    @Override
    public void onStart() {
        super.onStart();
        BOut.trace("Monitor::onStart");
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


    public void clickedButtonTalk(View v) {
        boolean talking = !((Bibi)getApplicationContext()).viewer.getClientIsTalking();
        ((Bibi)getApplicationContext()).viewer.setClientIsTalking(talking);
        if (talking) {
            v.setBackgroundColor(Color.RED);
            ((Button)v).setText("Transmitting ...");
        } else {
            v.setBackgroundResource(android.R.drawable.btn_default);
            //v.setBackgroundColor(talk_button_bg_color);
            ((Button)v).setText("Talk");
        }
    }
}
