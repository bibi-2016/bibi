package net.chavchi.android.bibi;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;

class BOut {
    private static final boolean print = true;
    private static final boolean trace = true;
    private static final boolean errors = true;

    private static void output(String s) {
        Log.d("bibi", s);
    }

    public static void print(String s) {
        if (print)
            output(s);
    }
    public static void println(String s) {
        print(s + "\n");
    }
    public static void trace(String s) {
        if (trace)
            output(s);
    }
    public static void error(String s) {
        if (errors)
            output(s);
    }
}


// global configuration
class Cfg {
    public String group = "local";  // name of the group
    public String name = "";   // device name

    public int port  = 9898;   // port for connections

    public boolean external_server_enabled = false;
    public String external_server_name = "192.168.192.123"; // external server to connect to

    public boolean detect_motion_enabled = false;
    public double detect_motion_threshold = 0.08;
    public boolean detect_sound_level_enabled = false;
    public double detect_sound_level_threshold = 0.5;
    public boolean accept_input_buttons_enabled = true;

    public int udp_search_timeout = 5000;
    public int event_duration_timeout = 2500;

    int camera_id = 1;
    int image_width = 320;
    int image_height = 240;

    int mic_sample_rate = 8000;

    Cfg() {
        name = BluetoothAdapter.getDefaultAdapter().getName();
        if (name == null || name.equals("")) {
            name = Build.BRAND + " " + Build.MODEL;
        }
    }
}


public class Bibi extends Application {
    private static Bibi singleton = null;
    public static Bibi getInstance() {
        return singleton;
    }

    public static Cfg cfg = new Cfg();

    //
    // Application class overrides
    //
    @Override
    public void onCreate () {
        super.onCreate();
        BOut.trace("Bibi::onCreate");

        // assign static members
        singleton = this;
    }

/*
    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        BOut.trace("Bibi::onConfigurationChanged");
    }

    @Override
    public void onLowMemory () {
        super.onLowMemory();
        BOut.trace("Bibi::onLowMemory");
    }

    @Override
    public void onTerminate () {
        super.onTerminate();
        BOut.trace("Bibi::onTerminate");
    }
*/

    //
    // Api
    //
    private enum RunningAs {
        None, Recorder, Viewer
    }

    private RunningAs running_as = RunningAs.None;

    public BMaster recorder = null;
    public BClient viewer = null;

    public void startRecorder() {
        BOut.trace("Bibi::startRecorder (" + running_as + ")");
        if (running_as == RunningAs.Recorder) {
            BOut.trace("Already running as recorder");
            return;
        }

        recorder = new BMaster(getApplicationContext());
        recorder.start();

        enableMediaButtons();

        running_as = RunningAs.Recorder;
    }

    public void startViewer() {
        BOut.trace("Bibi::startViewer (" + running_as + ")");
        if (running_as == RunningAs.Viewer) {
            BOut.trace("Already running as viewer");
            return;
        }

        viewer = new BClient(getApplicationContext());
        viewer.start();

        running_as = RunningAs.Viewer;
    }

    public void monitorMaster(String master) {
        viewer.monitorMaster(master == null ? null : viewer.communicator.getMaster(master));
    }

    private Activity monitorActivity = null;
    public void setMonitor(Activity m) {
        monitorActivity = m;
    }
    public void closeMonitor() {
        if (monitorActivity != null)
            monitorActivity.finish();
    }

        // stopAll
    //   called from main::onRestart or main::onDestroy
    //   have to stop the recorder or viewer
    public void stopAll() {
        BOut.trace("Bibi::stopAll (" + running_as + ")");

        try {
            switch (running_as) {
                case None:
                    break;
                case Recorder:
                    disableMediaButtons();
                    recorder.stop();
                    break;
                case Viewer:
                    monitorMaster(null);
                    viewer.stop();
                    break;
            }
        } catch (Exception ex) {
            BOut.error("Bibi::stopAll - " + ex);
        }

        recorder = null;
        viewer = null;

        running_as = RunningAs.None;
    }





    //
    // media buttons
    //   enable/disable the BBroadcastReceiver from receiving the button events
    //   the BBroadcastReceiver will call the onMediaButtonPress
    //
    public void enableMediaButtons() {
        BOut.trace("Bibi::enableMediaButtons");
        ((AudioManager)getSystemService(Context.AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(this, BBroadcastReceiver.class));
    }

    public void disableMediaButtons() {
        BOut.trace("Bibi::disableMediaButtons");
        ((AudioManager)getSystemService(Context.AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(new ComponentName(this, BBroadcastReceiver.class));
    }

    public void onMediaButtonPress(Intent intent) {
        if (running_as != RunningAs.Recorder)
            return;

        KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
            recorder.onAlarmButtonPressed();
        }
    }


    // askToExit
    //   displays dialog with provided message
    //   on Yes terminates the target activity
    public void askToExit(final Activity target, String message) {
        new AlertDialog.Builder(target)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        target.finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
