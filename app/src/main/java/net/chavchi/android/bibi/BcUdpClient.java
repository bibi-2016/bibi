package net.chavchi.android.bibi;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

interface BcUdpClientEvents {
    void onBcUdpClientMasterFound(String name, InetAddress address);
}

class BcUdpClient {
    private volatile boolean running = false;

    // input from ctor
    private BcUdpClientEvents callbacks = null;
    private int port = 0;

    private DatagramSocket socket = null;

    private Thread searcher_thread = null;
    private Thread worker_thread = null;


    // worker
    //   - waits for incomming connection on udp port
    //   - creates response packet from input packet (handshake)
    //   - sends response packet to connected client
    private Runnable worker = new Runnable() {
        public void run() {
            BOut.trace("BcUdpClient::worker::start");

            byte[] in_buff = new byte[1500];
            DatagramPacket in_packet = new DatagramPacket(in_buff, in_buff.length);

            try {
                while (true) {
                    socket.receive(in_packet);

                    final String msg = new String(in_packet.getData(), 0, in_packet.getLength());
                    if (msg.startsWith("Here:")) {
                        callbacks.onBcUdpClientMasterFound(msg.substring(5), in_packet.getAddress());
                    }
                }
            } catch (Exception ex) {
                BOut.println("BcUdpClient::worker - " + ex);
            }

            BOut.trace("BcUdpClient::searcher::stop");
        }
    };


    private Runnable searcher = new Runnable() {
        public void run() {
            BOut.trace("BcUdpClient::searcher::start");
            try {
                final String query = "Pakiga:" + Bibi.cfg.group;
                final DatagramPacket ou_packet = new DatagramPacket(query.getBytes(), query.length(), InetAddress.getByName("255.255.255.255"), port);

                int cnt = 0;

                final int max_cnt = Bibi.cfg.udp_search_timeout / 100;

                while (running) {
                    socket.send(ou_packet);

                    // sleep in pieces (so that thread.join is faster)
                    while(running && cnt++ < max_cnt) {
                        Thread.sleep(100);
                    }
                    cnt = 0;
                }
            } catch (Exception ex) {
                BOut.println("BcUdpClient::searcher - " + ex);
            }

            BOut.trace("BcUdpClient::searcher::stop");
        }
    };


    BcUdpClient(int port, BcUdpClientEvents e) {
        callbacks = e;
        this.port = port;
    }

    // start
    //   - starts the worker thread
    public void start() {
        if (running)
            return;
        BOut.trace("BcUdpClient::start");

        try {
            socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
        } catch (Exception ex) {
            BOut.println("BcUdpClient::init - " + ex);
        }

        // must be here, because the searcher thread is using it for while condition
        running = true;

        worker_thread = new Thread(worker, "BcUdpClient::worker");
        worker_thread.start();

        searcher_thread = new Thread(searcher, "BcUdpClient::searcher");
        searcher_thread.start();
    }

    // stop
    //   - closes the server socket and in turn stops the worker thread
    public void stop() throws Exception {
        if (!running)
            return;
        BOut.trace("BcUdpClient::stop");

        running = false;
        searcher_thread.join();

        socket.close();
        worker_thread.join();

        socket = null;
        worker_thread = null;
        searcher_thread = null;
    }
}
