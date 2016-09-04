package net.chavchi.android.bibi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

//
//
// TCP Client
//    - sends command/video/audio strems to Client
//    - receives command/audio stream from Client
//
//
interface BmTcpClientEvents {
    void addBmTcpClient(BmTcpClient c);              // called from client after the hanshake
    void removeBmTcpClient(BmTcpClient c);           // called from client on socket close

    void onBmTcpClientCommandReceived(BmTcpClient c, String cmd);
    void onBmTcpClientAudioReceived(BmTcpClient c, byte[] data, int pos, int len);
}

class BmTcpClient implements TcpCommunicatorEvents {
    // input from ctor
    private BmTcpClientEvents callbacks = null;
    private Socket socket = null;

    public TcpCommunicator communicator = null;

    public String name = null; // client name
    public String full_name = null; // name@ip

    private boolean closed_from_self = false;

    // ctor
    //   - assignes callbacks
    public BmTcpClient(Socket s, BmTcpClientEvents e) {
        callbacks = e;
        socket = s;

        // handshake
        try {
            // get temp streams for simples string read/write
            BufferedReader sr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream sw = new DataOutputStream(socket.getOutputStream());

            boolean handshake_ok = false;
            String msg = sr.readLine();
            if (msg.startsWith("Group:")) {
                String group = msg.substring(6);
                if (group.equals(Bibi.cfg.group)) { // group matches
                    msg = sr.readLine();
                    if (msg.startsWith("Name:")) {
                        name = msg.substring(5);
                        // send self name
                        sw.writeBytes("Pakiga:" + Bibi.cfg.name + "\n");
                        handshake_ok = true;
                    }
                }
            }

            if (!handshake_ok) { // problem
                close();
                return;
            }
        } catch (IOException ex) {
            BOut.println("tcp client handshake failed: " + ex);
            return;
        }

        //All OK ... add client to list and start communicaotr
        full_name = name + "@" + socket.getRemoteSocketAddress();

        callbacks.addBmTcpClient(this);
        communicator = new TcpCommunicator(socket, this);
    }

    // close
    public void close() {
        BOut.trace("BmTcpClient::close");
        try {
            closed_from_self = true;
            socket.close();
        } catch (Exception ex) {
            BOut.error("BmTcpClient::close - " + ex);
        }
    }


    @Override
    public void onTcpCommunicatorDied() {
        if (closed_from_self == false)
            callbacks.removeBmTcpClient(this);
    }

    @Override
    public void onTcpCommunicatorCommandReceived(int cid, String cmd) {
        callbacks.onBmTcpClientCommandReceived(this, cmd);
    }

    @Override
    public void onTcpCommunicatorAudioReceived(int cid, byte[] data, int pos, int len) {
        callbacks.onBmTcpClientAudioReceived(this, data, pos, len);
    }

    @Override
    public void onTcpCommunicatorImageReceived(int cid, byte[] data, int pos, int len) {
        // do nothing ...
    }
}
