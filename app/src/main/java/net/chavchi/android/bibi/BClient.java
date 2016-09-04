package net.chavchi.android.bibi;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

public class BClient implements BMicEvents, BcCommunicatorEvents {
    private volatile boolean running = false;

    private BSpeaker speaker = null;
    private BMic mic = null;
    public BcCommunicator communicator = null;

    private BImageView view = null;
    private boolean client_is_talking = false;

    private BcMaster monitoringMaster = null;
    private MastersAdapter mastersAdapter = null;

    private Context context;

    BClient(Context c) {
        context = c;
    }

    public void start() {
        if (running)
            return;
        BOut.trace("BClient::start");

        mic = new BMic(Bibi.cfg.mic_sample_rate, this);
        speaker = new BSpeaker(Bibi.cfg.mic_sample_rate);
        communicator = new BcCommunicator(Bibi.cfg.port, this);

        mic.start();
        speaker.start();
        communicator.start();

        running = true;
    }

    public void stop() throws Exception {
        if (!running)
            return;
        BOut.trace("BClient::stop");

        mic.stop();
        speaker.stop();
        communicator.stop();

        mic = null;
        speaker = null;
        communicator = null;

        running = false;
    }


    public void monitorMaster(BcMaster master) {
        monitoringMaster = master;
    }

    public void setImageView(BImageView w) {
        view = w;
    }
    public void setClientIsTalking(boolean t) {
        client_is_talking = t;
    }
    public boolean getClientIsTalking() {
        return client_is_talking;
    }

    public void setMastersAdapter(MastersAdapter a) {
        mastersAdapter = a;
    }

    //
    // BcTcpMaster interface implementation
    //
    @Override
    public void onBcCommunicatorMastersChanged() {
        if (mastersAdapter != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    BClient.this.mastersAdapter.notifyDataSetChanged();
                    if (monitoringMaster != null && monitoringMaster.isAlive() == false) {
                        ((Bibi) BClient.this.context).closeMonitor();
                    }
                }
            });
        }
    }

    @Override
    public void onBcCommunicatorCommandReceived(BcMaster m, String cmd) {
        //BOut.println("Have command '" + cmd + "' from " + m.full_name);
    }

    @Override
    public void onBcCommunicatorAudioReceived(BcMaster m, byte[] data, int pos, int len) {
        if (view != null && monitoringMaster == m) {
            speaker.play(data, pos, len);
        }
    }

    @Override
    public void onBcCommunicatorImageReceived(BcMaster m, byte[] data, int pos, int len) {
        if (view != null && monitoringMaster == m) {
            view.updateImage(BitmapFactory.decodeByteArray(data, pos, len));
        }
    }


    @Override
    public void onSoundFrameRecorded(byte[] data, int pos, int len, double loudness) {
        if (view != null && client_is_talking) {
            monitoringMaster.sendAudio(data, pos, len);
        }
    }
}
