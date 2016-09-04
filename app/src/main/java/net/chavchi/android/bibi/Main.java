package net.chavchi.android.bibi;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class Main extends AppCompatActivity {
    //
    // Activity lifecycle
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BOut.trace("Main:onCreate");

        // create user interface
        setContentView(R.layout.activity_main);
        // create a toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        // set the settings
        ((EditText)findViewById(R.id.edittext_groupname)).setText(Bibi.cfg.group);
        ((EditText) findViewById(R.id.edittext_devicename)).setText(Bibi.cfg.name);

        findViewById(R.id.button_advanced_settings).requestFocus();
    }
    @Override
    public void onRestart() {
        super.onRestart();
        BOut.trace("Main:onRestart");

        ((Bibi)getApplicationContext()).stopAll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BOut.trace("Main:onDestroy");
        ((Bibi)getApplicationContext()).stopAll();
    }

/*
    @Override
    public void onStart() {
        super.onStart();
        BOut.trace("Main:onStart");
    }
    @Override
    public void onResume() {
        super.onResume();
        BOut.trace("Main:onResume");
    }
    @Override
    public void onPause() {
        super.onPause();
        BOut.trace("Main:onPause");
    }
    @Override
    public void onStop() {
        super.onStop();
        BOut.trace("Main:onStop");
    }
*/

    /*
    //
    // options menu creation and handling
    //
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Toolbar tb = (Toolbar) findViewById(R.id.toolbar_main);
        tb.inflateMenu(R.menu.menu_main);

        tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_settings:
                BOut.println("Action settings");
                return true;
            case R.id.menu_action_exit:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/
    //
    // back button press
    //
    @Override
    public void onBackPressed() {
        Bibi.getInstance().askToExit(this, "Really quit?");
    }

    //
    // callbacks
    //
    public void clickedButtonRecorder(View v) {
        ((Bibi)getApplicationContext()).startRecorder();
        startActivity(new Intent(this, Recorder.class));
    }

    public void clickedButtonViewer(View v) {
        ((Bibi)getApplicationContext()).startViewer();
        startActivity(new Intent(this, Viewer.class));
    }

    public void clickedButtonAdvanced(View v) {
        startActivity(new Intent(this, Settings.class));
    }
}
