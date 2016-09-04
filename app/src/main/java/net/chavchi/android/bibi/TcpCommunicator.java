package net.chavchi.android.bibi;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

interface TcpCommunicatorEvents {
    void onTcpCommunicatorDied();
    void onTcpCommunicatorCommandReceived(int cid, String cmd);
    void onTcpCommunicatorAudioReceived(int cid, byte[] data, int pos, int len);
    void onTcpCommunicatorImageReceived(int cid, byte[] data, int pos, int len);
}

public class TcpCommunicator {
    private volatile boolean running = false;

    private TcpCommunicatorEvents callbacks = null;
    private Socket socket = null;

    private InputStream in_stream = null;
    private OutputStream ou_stream = null;

    private Queue data_in_queue = new Queue(1000000);

    private Queue cmmnd_out_queue = new Queue(1000);
    private Queue audio_out_queue = new Queue(500000);
    private Queue video_out_queue = new Queue(1000000);

    private Thread receiver_thread = null;
    private Thread parser_thread = null;
    private Thread sender_thread = null;

    private int client_id = 0;

    private volatile boolean must_find_header = true;
    private volatile boolean waiting_for_header = true;

    // receiver
    private Runnable receiver = new Runnable() {
        public void run() {
            BOut.trace("Communicator::receiver::start");

            byte[] data = new byte[65536];

            try {
                while (true) {
                    // get data ... wait for it ...
                    int cnt = in_stream.read(data, 0, data.length);
                    if (cnt < 0) // socket closed ....
                        break;

                    if (must_find_header == true) { // ERROR ON STREAM ... skip to start of new package
                        for (int i = 0; i < cnt - 7; i++) {
                            if (data[i] == (byte) 0x3c && data[i+1] == (byte) 0x3c && data[i+6] == (byte) 0x3e && data[i+7] == (byte) 0x3e) { // header OK ...
                                //found header
                                must_find_header = false;
                                waiting_for_header = true;
                                BOut.print("Header found ");
                                // write from header to queue
                                data_in_queue.clear();
                                data_in_queue.write(data, i, cnt - i);
                                break;
                            }
                        }
                    } else {
                        data_in_queue.write(data, 0, cnt);
                    }
                }
            } catch (Exception ex) {
                BOut.error("Communicator::receiver - " + ex);
            }

            receiverLoopDied();

            BOut.trace("Communicator::receiver::stop");
        }
    };

    private Runnable parser = new Runnable() {
        @Override
        public void run() {
            BOut.trace("Communicator::parser::start");

            byte[] data = new byte[500000];

            int cid = 0;
            int sid = 0;
            int need_len_more_bytes = 0;

            while (running == true) {
                if (must_find_header == true || data_in_queue.getCount() == 0) {
                    Thread.yield();
                    continue;
                }

                // process data from queue
                if (waiting_for_header == true) { // read header
                    if (data_in_queue.getCount() >= 8) { // have header
                        data_in_queue.read(data, 0, 8); // read header

                        if (data[0] == (byte) 0x3c && data[1] == (byte) 0x3c && data[6] == (byte) 0x3e && data[7] == (byte) 0x3e) { // header OK ...
                            cid = (int)data[2];
                            sid = (int)data[3];
                            need_len_more_bytes = ((data[4] & 0xff) << 8) | (data[5] & 0xff);

                            //BOut.println("Have stream " + stream_id + " from master id " + master_id + " of len " + need_len_more_bytes);

                            waiting_for_header = false;
                        } else {
                            must_find_header = true;
                        }
                    }
                }

                if (waiting_for_header == false) { // have header ... read data
                    int cnt = data_in_queue.read(data, 0, need_len_more_bytes);

                    if (cnt > 0) {
                        switch (sid) {
                            case 1: // command
                                receivedCommand(cid, data, 0, cnt);
                                break;
                            case 2: // audio
                                receivedAudio(cid, data, 0, cnt);
                                break;
                            case 3: // image
                                receivedImage(cid, data, 0, cnt);
                                break;
                        }

                        need_len_more_bytes -= cnt;
                        if (need_len_more_bytes == 0) { // end of packge
                            waiting_for_header = true;
                        } else if (need_len_more_bytes < 0) { // Error ...
                            BOut.println("READ TOO MANY BYTES ...");
                            must_find_header = true;
                        }
                    }
                }
            }
        }
    };

    // sender
    //   - waits for data in the out_queue and send it to out stream
    private Runnable sender = new Runnable() {
        public void run() {
            BOut.trace("Communicator::sender::start");

            byte[] data = new byte[65536];

            final int max_cnt = data.length - 8;
            int cnt = 0;

            int video_pass = 0;
            int stream_type = 0;

            try {
                while (running == true) {
                    // command has absolute priority
                    cnt = cmmnd_out_queue.read(data, 0, max_cnt);
                    stream_type = 1;
                    if (cnt == 0) {
                        // no command, see if audio, but only for certain amount of times, before the video gets its change
                        if (video_pass < 5) { // still have priority over video
                            cnt = audio_out_queue.read(data, 0, max_cnt);
                            stream_type = 2;
                            video_pass++;
                        }
                        // no audio ... check if video
                        if (cnt == 0) {
                            cnt = video_out_queue.read(data, 0, max_cnt);
                            stream_type = 3;
                            video_pass = 0;
                        }
                    }

                    // see if have something to send
                    if (cnt == 0) {
                        Thread.yield();
                    } else {
                        //BOut.print("Send " + cnt + " bytes");
                        final byte[] header = {(byte)0x3c, (byte)0x3c, (byte)client_id, (byte)stream_type, (byte)(cnt >> 8), (byte)cnt, (byte)0x3e, (byte)0x3e};
                        ou_stream.write(header, 0, 8);
                        ou_stream.write(data, 0, cnt);
                    }
                }
            } catch (Exception ex) {
                BOut.println("Communicator::sender - " + ex);
            }

            BOut.trace("Communicator::sender::stop");
        }
    };

    TcpCommunicator(Socket s, TcpCommunicatorEvents e) {
        callbacks = e;
        socket = s;

        try {
            socket.setReceiveBufferSize(65536);
            socket.setSendBufferSize(65536);

            in_stream = socket.getInputStream();
            ou_stream = socket.getOutputStream();

        } catch (Exception ex) {
            BOut.error("Communicator::ctor - " + ex);
        }

        receiver_thread = new Thread(receiver, "BcTcpMaster::receiver::" + socket.getRemoteSocketAddress().toString());
        sender_thread = new Thread(sender, "BcTcpMaster::sender::" + socket.getRemoteSocketAddress().toString());
        parser_thread = new Thread(parser, "BcTcpMaster::parser::" + socket.getRemoteSocketAddress().toString());

        running = true;

        receiver_thread.start();
        sender_thread.start();
        parser_thread.start();
    }

    // called from receiver thread when the while loop ends
    private void receiverLoopDied() {
        BOut.trace("TcpCommunicator::receiverLoopDied");
        running = false; // mark dead ...

        try {
            parser_thread.join();
            sender_thread.join();
        } catch (Exception ex) {
            BOut.error("TcpCommunicator::receiverLoopDied - " + ex);
        }

        socket = null;

        receiver_thread = null;
        parser_thread = null;
        sender_thread = null;

        callbacks.onTcpCommunicatorDied(); // tell world
    }

    // command audio and image receied from master
    private String input_command = "";
    private void receivedCommand(int cid, byte[] data, int pos, int len) {
        for (int i = 0; i < len; i++) {
            final byte b = data[pos + i];
            if (b == 0) { // end of string
                callbacks.onTcpCommunicatorCommandReceived(cid, input_command);
                input_command = "";
            } else {
                input_command += (char)b;
            }
        }
    }

    private int input_audio_stream_pos = 0;
    private int input_audio_size = 0;
    private byte[] input_audio_data = new byte[500000];
    private void receivedAudio(int cid, byte[] data, int pos, int len) {
        for (int i = 0; i < len; i++) {
            final byte b = data[pos + i];
            switch (input_audio_stream_pos++) {
                case 0:
                    input_audio_size = b;
                    break;
                case 1:
                    input_audio_size = ((input_audio_size & 0xff) << 8) | (b & 0xff);
                    if (input_audio_data.length < input_audio_size) {
                        input_audio_data = new byte[input_audio_size];
                    }
                    break;
                default:
                    input_audio_data[input_audio_stream_pos - 3] = b;
                    input_audio_size--;
                    if (input_audio_size == 0) { // got the whole image
                        callbacks.onTcpCommunicatorAudioReceived(cid, input_audio_data, 0, input_audio_stream_pos - 2);
                        input_audio_stream_pos = 0; // start at the beginning
                    }
                    break;
            }
        }
    }

    private int input_image_stream_pos = 0;
    private int input_image_size = 0;
    private byte[] input_image_data = new byte[500000];
    private void receivedImage(int cid, byte[] data, int pos, int len) {
        for (int i = 0; i < len; i++) {
            final byte b = data[pos + i];
            switch (input_image_stream_pos++) {
                case 0:
                    input_image_size = b;
                    break;
                case 1:
                    input_image_size = ((input_image_size & 0xff) << 8) | (b & 0xff);
                    if (input_image_data.length < input_image_size) {
                        input_image_data = new byte[input_image_size];
                    }
                    break;
                default:
                    input_image_data[input_image_stream_pos - 3] = b;
                    input_image_size--;
                    if (input_image_size == 0) { // got the whole image
                        callbacks.onTcpCommunicatorImageReceived(cid, input_image_data, 0, input_image_stream_pos - 2);
                        input_image_stream_pos = 0; // start at the beginning
                    }
                    break;
            }
        }
    }

    public void setClientID(int cid) {
        client_id = cid;
    }

    public void sendCommand(String cmd) {
        final byte[] zero = {0};
        final byte[] bytes = cmd.getBytes();
        cmmnd_out_queue.write(bytes, 0, bytes.length);
        cmmnd_out_queue.write(zero, 0, 1);
    }

    public void sendAudio(byte[] data, int pos, int len) {
        final byte[] header = {(byte)(len >> 8), (byte)len };
        audio_out_queue.write(header, 0, 2);
        audio_out_queue.write(data, pos, len);
    }

    public void sendImage(byte[] data, int pos, int len) {
        final byte[] header = {(byte)(len >> 8), (byte)len };
        video_out_queue.write(header, 0, 2);
        video_out_queue.write(data, pos, len);
    }
}
