package net.chavchi.android.bibi;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class BSpeaker {
    private volatile boolean running = false;

    // input args from ctor
    private int sample_rate = 0;

    private BSoundCodec dec = null;
    private AudioTrack player = null;

    byte[] dec_buff = new byte[16000];

    BSpeaker(int sample_rate) {
        this.sample_rate = sample_rate;
    }

    public void start() {
        if (running)
            return;
        BOut.trace("BSpeaker::start");

        // create decoder for input sound
        dec = new BSoundCodec(false, sample_rate);
        dec.start();

        // create player
        int buffer_size = AudioTrack.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        player = new AudioTrack(AudioManager.STREAM_MUSIC, sample_rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size*10, AudioTrack.MODE_STREAM);
        player.play();

        running = true;
    }

    public void stop() {
        if (!running)
            return;
        BOut.trace("BSpeaker::stop");

        player.stop();
        player.release();
        player = null;

        dec.stop();
        dec = null;

        running = false;
    }


    public void play(byte[] data, int pos, int len) {
        if (!running)
            return;

        //int dec_size = dec.process(data, pos, len, dec_buff, 0, dec_buff.length);
        //player.write(dec_buff, 0, dec_size);
        player.write(data, pos, len);
    }
}
