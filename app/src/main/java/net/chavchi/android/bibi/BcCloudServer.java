package net.chavchi.android.bibi;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


interface BcCloudServerEvents {
    void onBcCloudServerCommandReceived(int cid, String cmd);
    void onBcCloudServerAudioReceived(int cid, byte[] data, int pos, int len);
    void onBcCloudServerImageReceived(int cid, byte[] data, int pos, int len);
}

public class BcCloudServer implements TcpCommunicatorEvents {
    private volatile boolean running = false;

    private BcCloudServerEvents callbacks = null;

    private Socket socket = null;
    private TcpCommunicator communicator = null;

    public static final Object masters_lock = new Object();
    public List<BcCloudMaster> masters = new ArrayList<BcCloudMaster>();

    public BcCloudServer(BcCloudServerEvents e) {
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

                sw.writeBytes("Group:" + Bibi.cfg.group + "\nName:" + Bibi.cfg.name + "\nType:viewer\n");
                String msg = sr.readLine();
                if (msg.equals("OK") == false) {
                    socket.close();
                    return;
                }
            } catch (Exception ex) {
                BOut.println("BcCloudServer::start - " + ex);
                return;
            }

            BOut.print("Cloud connected ...");
            // All OK ... add master and run communicator

            running = true;
            communicator = new TcpCommunicator(socket, BcCloudServer.this);
        }
    };

    public void start() {
        if (running)
            return;

        new Thread(worker).start();

        running = true;
    }

    public void stop() {
        if (!running)
            return;

        try {
            socket.close();
        } catch (Exception ex) {
            BOut.error("BcCloudServer::stop - " + ex);
        }

        socket = null;
        running = false;
    }
/*
    //
    // sending data to clients
    //
    public void sendImage(byte[] data, int pos, int len) {
    }

    public void sendAudio(byte[] data, int pos, int len) {
        if (communicator != null) {
            communicator.setClientID(selected_master);
            communicator.sendAudio(data, pos, len);
        }
    }

    public void sendCommand(String str) {
        if (communicator != null) {
            communicator.setClientID(selected_master);
            communicator.sendCommand(str);
        }
    }
*/
    @Override
    public void onTcpCommunicatorDied() {
        communicator = null;
    }

    @Override
    public void onTcpCommunicatorCommandReceived(int cid, String cmd) {

       // if (cmd.startsWith("Recorders=")) { // getting the list of recorders
        //    synchronized (masters_lock) {
         //       String[] parts = cmd.substring(10).split("\t");
          //      BOut.print("Have " + parts.length + " recorders");
/*
                BcCloudMaster old = null;
                for (int p = 0; p < parts.length; p++) {

                    for (int m = 0; m < masters.size(); m++) {
                        if (masters.get(m).name.equals(parts[p])) { // found recorder on the list
                            old = masters.remove(m);
                            break;
                        }
                    }

                    if (old != null) { // master already there, add it to the correct index
                        masters.add(p, old);
                    } else { // must add new mater
                        masters.add(p, new BcCloudMaster(parts[p], parts[p] + "@cloud", this);
                    }
                }
 */
 /*           }
        } else if (cmd.startsWith("Event=")) { // getting event for the
            masters.get(cid).events.parse(cmd.substring(6));
        }
        */
        BOut.println("HAVE command: " + cmd + ": from " + cid);
        //callbacks.onBcCloudServerCommandReceived(cid, cmd);
    }

    @Override
    public void onTcpCommunicatorAudioReceived(int cid, byte[] data, int pos, int len) {
        BOut.println("Have " + len + " bytes of audo from " + cid);
        //callbacks.onBcCloudServerAudioReceived(cid, data, pos, len);
    }

    @Override
    public void onTcpCommunicatorImageReceived(int cid, byte[] data, int pos, int len) {
        BOut.print("Have image from " + cid);
        //callbacks.onBcCloudServerImageReceived(cid, data, pos, len);
    }
}
