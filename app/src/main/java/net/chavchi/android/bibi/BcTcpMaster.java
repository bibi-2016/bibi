package net.chavchi.android.bibi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

interface BcTcpMasterEvents extends BcMasterEvents {
    void addBcTcpMaster(BcTcpMaster m);
    void removeBcTcpMaster(BcTcpMaster m);

    void onBcTcpMasterCommandReceived(BcTcpMaster m, String cmd);
    void onBcTcpMasterAudioReceived(BcTcpMaster m, byte[] data, int pos, int len);
    void onBcTcpMasterImageReceived(BcTcpMaster m, byte[] data, int pos, int len);
}

class BcTcpMaster extends BcMaster implements TcpCommunicatorEvents {
    // input from ctor
    private BcTcpMasterEvents callbacks;

    private Socket socket = null;

    private boolean closed_from_self = false;

    public BcTcpMaster(String name, String full_name, InetAddress addr, int port, BcTcpMasterEvents e) {
        super(name, full_name, e);

        callbacks = e;

        try {
            // connect to master
            socket = new Socket(addr, port);

            //get temp streams for handshake
            BufferedReader sr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream sw = new DataOutputStream(socket.getOutputStream());

            //send hello banner
            boolean handshake_ok = false;
            sw.writeBytes("Group:" + Bibi.cfg.group + "\nName:" + Bibi.cfg.name + "\n");
            String msg = sr.readLine();
            if (msg.startsWith("Pakiga:")) {
                if (msg.substring(7).equals(name)) {
                    handshake_ok = true;
                }
            }

            if (!handshake_ok) {
                socket.close();
                return;
            }
        } catch (Exception ex) {
            BOut.println("BcTcpMaster::ctor - " + ex);
            return;
        }

        BOut.print("Master added ... ");

        // All OK ... add master and run communicator
        super.start();

        callbacks.addBcTcpMaster(this);
        communicator = new TcpCommunicator(socket, this); // on base class
    }

    public void close() {
        BOut.trace("BcTcpMaster::close");
        try {
            closed_from_self = true;
            socket.close(); //closes socket ... kills reciver thread and set running = false, which kills sender thread
        } catch (Exception ex) {
            BOut.println("BcTcpMaster::close - " + ex);
        }

        socket = null;
    }

    @Override
    public void onTcpCommunicatorDied() {
        BOut.trace("BcTcpMaster::onTcpCommunicatorDied - " + closed_from_self);

        super.stop();

        if (closed_from_self == false) {
            callbacks.removeBcTcpMaster(this);
        }
    }

    @Override
    public void onTcpCommunicatorCommandReceived(int cid, String cmd) {
        if (cmd.startsWith("Event=")) { // handle events locally
            events.parse(cmd.substring(6));
        } else {
            callbacks.onBcTcpMasterCommandReceived(this, cmd);
        }
    }

    @Override
    public void onTcpCommunicatorAudioReceived(int cid, byte[] data, int pos, int len) {
        callbacks.onBcTcpMasterAudioReceived(this, data, pos, len);
    }

    @Override
    public void onTcpCommunicatorImageReceived(int cid, byte[] data, int pos, int len) {
        callbacks.onBcTcpMasterImageReceived(this, data, pos, len);
    }
}

