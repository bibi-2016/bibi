package net.chavchi.android.bibi;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


/*
//
// this is a proper aac coder/decoder build using the mediaCodec library.
// the mediacodec requires api 16. and this implementation has sigificant
// delay (~1s), while the compression for the 8000bit sample rate is around
// 2.6 : 1
//
public class BSoundCodec {
    private BApp app = BApp.getApp();
    // init and play flags
    private boolean valid = false;
    private volatile boolean running = false;

    private boolean is_encoder;
    private int sample_rate;

    private final String c_type = "audio/mp4a-latm";

    private MediaCodec codec;
    private ByteBuffer[] cin_buffers;
    private ByteBuffer[] cou_buffers;
    private MediaCodec.BufferInfo b_info;

    private MediaFormat getFormat(boolean encoder) {
        MediaFormat format = MediaFormat.createAudioFormat(c_type, sample_rate, 1);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        if (encoder == true) {
            format.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
        } else {
            format.setInteger(MediaFormat.KEY_IS_ADTS, 0);
        }

        return format;
    }

    private MediaCodec createCodec() throws Exception {
        MediaCodec c = null;
        if (is_encoder == true) {
            c = MediaCodec.createEncoderByType(c_type);
            c.configure(getFormat(true), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } else {
            c = MediaCodec.createDecoderByType(c_type);
            c.configure(getFormat(false), null, null, 0);
        }

        return c;
    }

    BSoundCodec(boolean encoder, int sample_rate) {
        this.sample_rate = sample_rate;
        is_encoder = encoder;
    }

    // init
    //   - create an encoder
    public void init() {
        if (valid)
            return;

        try {
            codec = createCodec();
            b_info = new MediaCodec.BufferInfo();
        } catch (Exception ex) {
            BOut.error("BSoundCodec::init() - " + ex);
        }

        valid = true;
    }

    // start
    //   - start encoder and get its in/out buffers
    public void start() {
        if (!valid || running)
            return;

        codec.start();

        cin_buffers = codec.getInputBuffers();
        cou_buffers = codec.getOutputBuffers();

        running = true;
    }

    public void stop() {
        if (!valid || !running)
            return;

        codec.stop();

        running = false;
    }

    public void term() {
        if (!valid)
            return;

        stop();
        codec.release();
    }

    // process
    //   - will encode/decode (depends on ctor argument of object) the data in in_data array
    //   - result will be placed in the ou_data array and the ou_pos position
    //   - number of bytes added in the ou_data is returned by the function
    //   !! ou_data must be allocated outside with the sufficient size - there is no checking done in the function
    public int process(byte[] in_data, int in_pos, int in_len, byte[] ou_data, int ou_pos, int ou_len) {
        if (in_len == 0)
            return 0;

        // set data to be encoded
        int iid = codec.dequeueInputBuffer(0);
        if (iid >= 0) { // have empty input buffer
            ByteBuffer c_buff = cin_buffers[iid];
            c_buff.clear();
            c_buff.put(in_data, in_pos, in_len);

            codec.queueInputBuffer(iid, 0, in_len, 0, 0);
        }

        // get decoded data back
        int ou_data_size = ou_pos; // where to put the data in the out buffer
        int oid = codec.dequeueOutputBuffer(b_info, 0);
        while (oid >= 0 || oid == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            if (oid == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                cou_buffers = codec.getOutputBuffers();
            } else {
                ByteBuffer c_buff = cou_buffers[oid];
                c_buff.position(b_info.offset);
                c_buff.get(ou_data, ou_data_size, b_info.size);

                ou_data_size += b_info.size;

                codec.releaseOutputBuffer(oid, false);
            }
            // check if there is more data
            oid = codec.dequeueOutputBuffer(b_info, 0);
        }

        return ou_data_size - ou_pos; // substract ou_pos to get the actual byte count
    }
}
*/


//
// this is a simple compressoin SoundCodec
// data is compressed/decompressed with zlib algorithm
// delay is shorter than with the aac codec, but the compression
// ratio isjust 1.22 : 1
//
public class BSoundCodec {
    private volatile boolean running = false;

    private boolean is_encoder;

    private Deflater compresser = null;
    private Inflater decompresser = null;

    BSoundCodec(boolean encoder, int sample_rate) {
        is_encoder = encoder;
    }

    // start
    //   - start encoder and get its in/out buffers
    public void start() {
        if (running)
            return;
        BOut.trace("BSoundCodec::start");

        try {
            if (is_encoder == true) {
                compresser = new Deflater(9);
            } else {
                decompresser = new Inflater();
            }
        } catch (Exception ex) {
            BOut.error("BSoundCodec::start - " + ex);
        }

        running = true;
    }

    public void stop() {
        if (!running)
            return;

        BOut.trace("BSoundCodec::stop");

        if (is_encoder == true) {
            compresser.end();
            compresser = null;
        } else {
            decompresser.end();
            decompresser = null;
        }

        running = false;
    }

    // process
    //   - will encode/decode (depends on ctor argument of object) the data in in_data array
    //   - result will be placed in the ou_data array and the ou_pos position
    //   - number of bytes added in the ou_data is returned by the function
    //   !! ou_data must be allocated outside with the sufficient size - there is no checking done in the function
    public int process(byte[] in_data, int in_pos, int in_len, byte[] ou_data, int ou_pos, int ou_len) {
        if (in_len == 0)
            return 0;

        int ou_size = 0;

        if (is_encoder == true) {
            compresser.setInput(in_data, in_pos, in_len);
            compresser.finish();
            ou_size = compresser.deflate(ou_data, ou_pos, ou_len);
            compresser.reset();
        } else {
            try {
                decompresser.setInput(in_data, in_pos, in_len);
                ou_size = decompresser.inflate(ou_data, ou_pos, ou_len);
                decompresser.reset();
            } catch (Exception ex) {
                BOut.error("BSoundCodec::process - " + ex);
                ou_size = 0;
            }
        }

        return ou_size;
    }
}
