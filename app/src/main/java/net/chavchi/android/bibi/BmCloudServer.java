package net.chavchi.android.bibi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

interface BmCloudServerEvents {
    void onBmCloudServerCommandReceived(int cid, String cmd);
    void onBmCloudServerAudioReceived(int cid, byte[] data, int pos, int len);
}

public class BmCloudServer implements TcpCommunicatorEvents {
    private volatile boolean running = false;

    private BmCloudServerEvents callbacks = null;

    private Socket socket = null;
    private TcpCommunicator communicator = null;

    public BmCloudServer(BmCloudServerEvents e) {
        callbacks = e;
    }

    private Runnable worker = new Runnable() {
        @Override
        public void run() {
            try {
                // connect to server
                socket = new Socket(Bibi.cfg.external_server_name, Bibi.cfg.port);

                //get temp streams for handshake
                BufferedReader sr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream sw = new DataOutputStream(socket.getOutputStream());

                sw.writeBytes("Group:" + Bibi.cfg.group + "\nName:" + Bibi.cfg.name + "\nType:recorder\n");
                String msg = sr.readLine();
                //BOut.println("Send msg ... got - " + msg);
                if (msg.equals("OK") == false) {
                    socket.close();
                    return;
                }
            } catch (Exception ex) {
                BOut.println("BmCloudServer::start - " + ex);
                return;
            }

            BOut.print("Cloud connected ...");
            // All OK ... add master and run communicator
            running = true;

            communicator = new TcpCommunicator(socket, BmCloudServer.this);
        }
    };

    public void start() {
        if (running)
            return;
        BOut.trace("BmCloudServer::start");

        new Thread(worker).start();

        running = true;
    }

    public void stop() {
        if (!running)
            return;
        BOut.trace("BmCloudServer::stop");

        try {
            socket.close();
        } catch (Exception ex) {
            BOut.error("BmCloudServer::stop - " + ex);
        }

        socket = null;
        running = false;
    }

    //
    // sending data to clients
    //
    public void sendImage(byte[] data, int pos, int len) {
        if (communicator != null)
            communicator.sendImage(data, pos, len);
    }

    public void sendAudio(byte[] data, int pos, int len) {
        if (communicator != null)
            communicator.sendAudio(data, pos, len);
    }

    public void sendCommand(String str) {
        if (communicator != null)
            communicator.sendCommand(str);
    }

    @Override
    public void onTcpCommunicatorDied() {
        communicator = null;
    }

    @Override
    public void onTcpCommunicatorCommandReceived(int cid, String cmd) {
        callbacks.onBmCloudServerCommandReceived(cid, cmd);
    }

    @Override
    public void onTcpCommunicatorAudioReceived(int cid, byte[] data, int pos, int len) {
        callbacks.onBmCloudServerAudioReceived(cid, data, pos, len);
    }

    @Override
    public void onTcpCommunicatorImageReceived(int cid, byte[] data, int pos, int len) {

    }
}
