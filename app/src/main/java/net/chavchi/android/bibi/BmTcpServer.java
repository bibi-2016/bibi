package net.chavchi.android.bibi;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//
//
// TCP Server
//
//
interface BmTcpServerEvents {
    void onBmTcpServerCommandReceived(BmTcpClient c, String cmd);
    void onBmTcpServerAudioReceived(BmTcpClient c, byte[] data, int pos, int len);
}

class BmTcpServer implements BmTcpClientEvents {

    private volatile boolean running = false;
    // input from ctor
    private BmTcpServerEvents callbacks = null;
    private int port = 0;

    private ServerSocket server_socket = null;
    private Thread server_thread = null;

    private final ReentrantReadWriteLock clients_rwl = new ReentrantReadWriteLock();
    private final Lock clients_wl = clients_rwl.writeLock();
    private final Lock clients_rl = clients_rwl.readLock();
    private List<BmTcpClient> clients = new ArrayList<BmTcpClient>();

    // worker
    //   - waits for incoming connectoins on tcp port
    //   - spawns new TcpClient with each incpming connection
    //      - handshake, data in/out is handled by tcpclient
    //   - when server socket closes, all clients gets closed
    private Runnable worker = new Runnable() {
        public void run() {
            BOut.trace("BmTcpServer::worker::start");

            try {
                while (true) {
                    new BmTcpClient(server_socket.accept(), BmTcpServer.this); // create tcp client with new accepted socket and this server as its owner
                }
            } catch (Exception ex) {
                BOut.error("BmTcpServer::worker - " + ex);
            }

            // not alive any more ... close all sockets
            for (int i = 0; i < clients.size(); i++) {
                clients.get(i).close();
            }

            BOut.trace("BmTcpServer::worker::stop");
        }
    };

    // ctor
    //  - assigns callbacks and set read listening port from config
    public BmTcpServer(int port , BmTcpServerEvents e) {
        callbacks = e;
        this.port = port;
    }

    // start
    //   - starts the worker thread
    public void start() {
        if (running)
            return;
        BOut.trace("BmTcpServer::start");

        try {
            server_socket = new ServerSocket(port);
        } catch (Exception ex) {
            throw new RuntimeException("BmTcpServer::start - " + ex);
        }

        server_thread = new Thread(worker, "BMTCPServer");
        server_thread.start();

        running = true;
    }

    // stop
    //   - closes the server socket and in turn closes all the clients and stopes the thread
    public void stop() {
        if (!running)
            return;
        BOut.trace("BmTcpServer::stop");

        try {
            server_socket.close();
            server_thread.join();
        } catch (Exception ex) {
            BOut.error("BmTcpServer::stop - " + ex);
        }

        server_socket = null;
        server_thread = null;

        running = false;
    }

    //
    // BmTcpClientEvents interface implementation
    //
    public void addBmTcpClient(BmTcpClient c) {
        BOut.trace("BmTcpServer::addTcpClient " + c.full_name);
        boolean must_add = true;

        clients_wl.lock();
        try {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).full_name.equals(c.full_name)) { // client allready connected
                    c.close();
                    must_add = false;
                    break;
                }
            }
            if (must_add) {
                clients.add(c);
            }
        } finally {
            clients_wl.unlock();
        }
    }

    @Override
    public void removeBmTcpClient(BmTcpClient c) {
        BOut.trace("BmTcpServer::removeTcpClient " + c.full_name);

        clients_wl.lock();
        clients.remove(c);
        clients_wl.unlock();
    }

    @Override
    public void onBmTcpClientCommandReceived(BmTcpClient c, String cmd) {
        callbacks.onBmTcpServerCommandReceived(c, cmd);
    }

    @Override
    public void onBmTcpClientAudioReceived(BmTcpClient c, byte[] data, int pos, int len) {
        callbacks.onBmTcpServerAudioReceived(c, data, pos, len);
    }

    //
    // sending data to clients
    //
    public void sendImage(byte[] data, int pos, int len) {
        clients_rl.lock();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).communicator.sendImage(data, pos, len);
        }
        clients_rl.unlock();
    }

    public void sendAudio(byte[] data, int pos, int len) {
        clients_rl.lock();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).communicator.sendAudio(data, pos, len);
        }
        clients_rl.unlock();
    }

    public void sendCommand(String str) {
        clients_rl.lock();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).communicator.sendCommand(str);
        }
        clients_rl.unlock();
    }
}

