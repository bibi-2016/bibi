package net.chavchi.android.bibi;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

interface BMicEvents {
    void onSoundFrameRecorded(byte[] data, int pos, int len, double loudness);
}

public class BMic {
    private volatile boolean running = false;

    // input args from ctor
    private BMicEvents callbacks = null;
    private int sample_rate = 0;

    private AudioRecord recorder = null;
    private byte[] rec_buffer = null;
    private BSoundCodec enc = null;

    private Thread thread = null;

    private Runnable worker = new Runnable() {
        public void run() {
            double volume = 0;

            BOut.trace("BMic::worker::start");

            final int read_request_size = sample_rate / 10; // 100 ms
            try {
                while (running) {
                    int read_size = recorder.read(rec_buffer, 0, read_request_size);
                    if (read_size > 0) {
                        // calc volume
                        volume = 0;
                        for (int i = 0; i < read_size; i += 2) {
                            short sample = (short) ((((short) rec_buffer[i + 1]) << 8) | (0xff & rec_buffer[i + 0]));
                            volume += sample * sample;
                        }
                        volume = Math.sqrt(volume / (read_size >> 1)) / 3276.8;

                        // encode the sound frame
                        //int enc_size = enc.process(rec_buffer, 0, read_size, rec_buffer, 0, rec_buffer.length);
                        //callbacks.onSoundFrameRecorded(rec_buffer, 0, enc_size, volume);
                        callbacks.onSoundFrameRecorded(rec_buffer, 0, read_size, volume);
                    }
                }
            } catch (Exception ex) {
                BOut.error("BMic::worker - " + ex);
            }

            BOut.trace("BMic::worker::stop");
        }
    };

    BMic(int sample_rate, BMicEvents e) {
        callbacks = e;
        this.sample_rate = sample_rate;
    }

    public void start() {
        if (running)
            return;

        BOut.trace("BMic::start");

        // create sound encoder
        enc = new BSoundCodec(true, sample_rate);
        enc.start();

        // create recorder
        int buffer_size = AudioRecord.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        rec_buffer = new byte[buffer_size * 10];
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size*10);
        recorder.startRecording();

        // start worker thread
        running = true;
        thread = new Thread(worker, "BMic");
        thread.start();
    }

    public void stop() throws Exception {
        if (!running)
            return;

        BOut.trace("BMic::stop");

        running = false;
        thread.join();
        thread = null;

        recorder.stop();
        recorder.release();
        recorder = null;

        enc.stop();
        enc = null;
    }
}
