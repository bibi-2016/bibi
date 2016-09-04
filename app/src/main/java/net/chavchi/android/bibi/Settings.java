package net.chavchi.android.bibi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BOut.trace("Settings:onCreate");

        // create user interface
        setContentView(R.layout.activity_settings);
        // create a toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_settings));
        // enable the back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // fill the editboxes
        ((EditText)findViewById(R.id.editText_port_number)).setText(String.valueOf(Bibi.cfg.port));
        ((EditText)findViewById(R.id.editText_external_server_address)).setText(Bibi.cfg.external_server_name);
        CheckBox cb = null;

        cb = (CheckBox)findViewById(R.id.checkBox_external_server_enable);
        cb.setChecked(Bibi.cfg.external_server_enabled);
        clickedExternalServerEnableCheckbox(cb);

        cb = (CheckBox)findViewById(R.id.checkBox_use_front_camera);
        cb.setChecked(Bibi.cfg.camera_id != 0);

        ((EditText)findViewById(R.id.editText_detect_motion_threshold)).setText(String.valueOf(Bibi.cfg.detect_motion_threshold));
        cb = (CheckBox)findViewById(R.id.checkBox_detect_motion_enable);
        cb.setChecked(Bibi.cfg.detect_motion_enabled);
        clickedDetectMotionEnableCheckbox(cb);

        ((EditText)findViewById(R.id.editText_detect_sound_level_threshold)).setText(String.valueOf(Bibi.cfg.detect_sound_level_threshold));
        cb = (CheckBox)findViewById(R.id.checkBox_detect_sound_level_enable);
        cb.setChecked(Bibi.cfg.detect_sound_level_enabled);
        clickedDetectSoundLevelEnableCheckbox(cb);

        cb = (CheckBox)findViewById(R.id.checkBox_accept_input_buttons_enable);
        cb.setChecked(Bibi.cfg.accept_input_buttons_enabled);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BOut.trace("Settings:onStop");

        Bibi.cfg.port = Integer.parseInt(((EditText) findViewById(R.id.editText_port_number)).getText().toString());
        Bibi.cfg.external_server_enabled = ((CheckBox)findViewById(R.id.checkBox_external_server_enable)).isChecked();
        Bibi.cfg.external_server_name = ((EditText)findViewById(R.id.editText_external_server_address)).getText().toString();

        Bibi.cfg.camera_id = ((CheckBox)findViewById(R.id.checkBox_use_front_camera)).isChecked() ? 1 : 0;

        Bibi.cfg.detect_motion_enabled = ((CheckBox)findViewById(R.id.checkBox_detect_motion_enable)).isChecked();
        Bibi.cfg.detect_motion_threshold = Double.parseDouble(((EditText) findViewById(R.id.editText_detect_motion_threshold)).getText().toString());
        Bibi.cfg.detect_sound_level_enabled = ((CheckBox)findViewById(R.id.checkBox_detect_sound_level_enable)).isChecked();
        Bibi.cfg.detect_sound_level_threshold = Double.parseDouble(((EditText) findViewById(R.id.editText_detect_sound_level_threshold)).getText().toString());
        Bibi.cfg.accept_input_buttons_enabled = ((CheckBox)findViewById(R.id.checkBox_accept_input_buttons_enable)).isChecked();
    }


    public void clickedExternalServerEnableCheckbox(View v) {
        boolean checked = ((CheckBox)v).isChecked();
        ((EditText)findViewById(R.id.editText_external_server_address)).setEnabled(checked);
        ((TextView)findViewById(R.id.textView_external_server_address)).setEnabled(checked);
    }
    public void clickedDetectMotionEnableCheckbox(View v) {
        boolean checked = ((CheckBox)v).isChecked();
        ((EditText)findViewById(R.id.editText_detect_motion_threshold)).setEnabled(checked);
        ((TextView)findViewById(R.id.textView_detect_motion_threshold)).setEnabled(checked);
    }
    public void clickedDetectSoundLevelEnableCheckbox(View v) {
        boolean checked = ((CheckBox)v).isChecked();
        ((EditText)findViewById(R.id.editText_detect_sound_level_threshold)).setEnabled(checked);
        ((TextView)findViewById(R.id.textView_detect_sound_level_threshold)).setEnabled(checked);
    }
}
