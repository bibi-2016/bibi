package net.chavchi.android.bibi;


import android.support.annotation.Nullable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

//
//
// UDP Server
//
//
interface BmUdpServerEvents {
}

class BmUdpServer {
    private volatile boolean running = false;

    // input from ctor
    private BmUdpServerEvents callbacks = null;
    private int port = 0;

    private DatagramSocket socket = null;
    private Thread worker_thread = null;

    // worker
    //   - waits for incomming connection on udp port
    //   - creates response packet from input packet (handshake)
    //   - sends response packet to connected client
    private Runnable worker = new Runnable() {
        public void run() {
            BOut.trace("BmUdpServer::worker::start");

            byte[] in_buff = new byte[1500];
            DatagramPacket in_packet = new DatagramPacket(in_buff, in_buff.length);

            try {
                while (true) {
                    socket.receive(in_packet);

                    DatagramPacket packet = createUDPResponse(in_packet);
                    if (packet != null) {
                        socket.send(packet);
                    }
                }
            } catch (Exception ex) {
                BOut.error("BmUdpServer::worker - " + ex);
            }

            BOut.trace("BmUdpServer::worker::stop");
        }
    };

    // createUDPResponse
    //   - creates the proper response for incomming challenge in the in_packet
    //   - if incomming challenge is not OK, return null
    @Nullable
    private DatagramPacket createUDPResponse(DatagramPacket in_packet) {
        String msg = new String(in_packet.getData(), 0, in_packet.getLength());

        //BOut.println("Got '" + msg + "' from " + in_packet.getAddress() + ":" + in_packet.getPort());

        if (msg.equals("Pakiga:" + Bibi.cfg.group)) { // have request for this group
            final String ans = "Here:" + Bibi.cfg.name;
            //BOut.println("Send answer '" + ans + "' len " + ans.length());
            return new DatagramPacket(ans.getBytes(), ans.length(), in_packet.getAddress(), in_packet.getPort());
        }

        return null;
    }

    // ctor
    //   - assign callbacks and read listening port from config
    public BmUdpServer(int port, BmUdpServerEvents e) {
        callbacks = e;
        this.port = port;
    }

    // start
    //   - starts the worker thread
    public void start() {
        if (running)
            return;
        BOut.trace("BmUdpServer::start");

        try {
            socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
        } catch (Exception ex) {
            BOut.error("BmUdpServer::init - " + ex);
            return;
        }

        worker_thread = new Thread(worker, "BMUDPServer");
        worker_thread.start();

        running = true;
    }

    // stop
    //   - closes the server socket and in turn stops the worker thread
    public void stop() {
        if (!running)
            return;
        BOut.trace("BmUdpServer::stop");

        try {
            socket.close();
            worker_thread.join();
        } catch (Exception ex) {
            BOut.error("BmUdpServer::stop - " + ex);
        }

        socket = null;
        worker_thread = null;

        running = false;
    }
}
