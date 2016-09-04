package net.chavchi.android.bibi;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

class BMaster implements BCameraEvents, BMicEvents, BmCommunicatorEvents, BEventsEvents {
    private volatile boolean running = false;

    private BCamera camera = null;
    private BMic mic = null;
    private BSpeaker speaker = null;

    public BmCommunicator communicator = null;

    private Context context = null;

    private VideoStats video_stats = null;
    private BImageView image_view = null;

    private BEvents events = null;

    BMaster(Context c) {
        context = c;
        video_stats = new VideoStats();
    }

    // set image view ... where to display the image and sound level
    public void setImageView(BImageView w) {
        image_view = w;
    }

    //
    // status text
    //
    private TextView status_text_view = null;
    private String status_text = "";

    public void setStatusTextView(TextView v) {
        status_text_view = v;
    }
    private void setStatusText(String msg) {
        if (status_text_view == null)
            return;

        status_text = msg;
        ((Activity)status_text_view.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status_text_view.setText(status_text);
            }
        });
    }



    public void start() {
        if (running)
            return;
        BOut.trace("BMaster::start");

        running = true;

        events = new BEvents(this);
        camera = new BCamera(Bibi.cfg.camera_id, Bibi.cfg.image_width, Bibi.cfg.image_height, this, context);
        mic = new BMic(Bibi.cfg.mic_sample_rate, this);
        speaker = new BSpeaker(Bibi.cfg.mic_sample_rate);
        communicator = new BmCommunicator(Bibi.cfg.port, this);

        camera.start();
        mic.start();
        speaker.start();
        communicator.start();
        events.start();
    }

    public void stop() throws Exception {
        if (!running)
            return;

        BOut.trace("BMaster::stop");

        running = false;

        events.stop();
        camera.stop();
        mic.stop();
        speaker.stop();
        communicator.stop();

        camera = null;
        mic = null;
        speaker = null;
        communicator = null;
        events = null;
    }


    // called from Camera when the new frame is available
    @Override
    public void onImageCaptured(BImage img) {
        if (img == null) {
            return;
        }

        VideoStats.Stats movement = null;
        if (Bibi.cfg.detect_motion_enabled) {
            // calculate image statistics
            video_stats.processNewImage(img);

            // check for motion
            if (video_stats.haveMovement(Bibi.cfg.detect_motion_threshold)) {
                if (events.setEvent(BEvents.Motion)) {
                    communicator.sendCommand("Event=Motion");
                }
            }

            movement = video_stats.movement;
        }

        // display image on master view
        if (image_view != null)
            image_view.updateImage(img, movement);

        // compress image
        byte[] data = img.getAsJpeg();
        if (data == null) {
            BOut.print("Compress failed!!!!");
            return;
        }
        // send compressed image out
        communicator.sendImage(data, 0, data.length);
    }

    // called from Mic when the new sound frame is recorded
    @Override
    public void onSoundFrameRecorded(byte[] data, int pos, int len, double volume) {
        //check for sound trigger
        if (Bibi.cfg.detect_sound_level_enabled) {
            if (volume > Bibi.cfg.detect_sound_level_threshold) {
                if (events.setEvent(BEvents.Sound)) {
                    communicator.sendCommand("Event=Sound");
                };
            }
        }

        // display loudness on master view
        if (image_view != null)
            image_view.updateVolume(volume);

        // send compressed sound out
        communicator.sendAudio(data, pos, len);
    }

    // called from Bibi when media button is pressed
    public void onAlarmButtonPressed() {
        if (Bibi.cfg.accept_input_buttons_enabled) {
            if (events.setEvent(BEvents.Alarm)) {
                communicator.sendCommand("Event=Alarm");
            };
        }
    }

    // called from communication class
    @Override
    public void onBmCommunicatorCommandReceived(BmTcpClient c, String cmd) {
        BOut.println("Got cmd " + cmd + " from " + (c == null ? "CLOUD" : c.full_name));
    }

    @Override
    public void onBmCommunicatorAudioReceived(BmTcpClient c, byte[] buff, int pos, int len) {
        // play it on speakers
        speaker.play(buff, 0, len);
    }

    @Override
    public void onBEventsEventsChanged(String status) {
        setStatusText(status);
    }
}

