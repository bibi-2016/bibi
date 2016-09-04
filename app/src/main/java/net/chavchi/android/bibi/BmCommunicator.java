package net.chavchi.android.bibi;

//
//
// Main class for network communication
//
//
interface BmCommunicatorEvents {
    void onBmCommunicatorCommandReceived(BmTcpClient c, String cmd);
    void onBmCommunicatorAudioReceived(BmTcpClient c, byte[] data, int pos, int len);
}

class BmCommunicator implements BmUdpServerEvents, BmTcpServerEvents, BmCloudServerEvents {
    private volatile boolean running = false;

    // input from ctor
    private BmCommunicatorEvents callbacks = null;
    private int port = 0;

    private BmUdpServer udp_server = null;
    private BmTcpServer tcp_server = null;
    private BmCloudServer cloud_server = null;

    BmCommunicator(int port, BmCommunicatorEvents e) {
        callbacks = e;
        this.port = port;
    }

    //
    // API
    //
    public void start() {
        if (running)
            return;
        BOut.trace("BmCommunicator::start");

        udp_server = new BmUdpServer(port, this);
        tcp_server = new BmTcpServer(port, this);
        cloud_server = new BmCloudServer(this);

        udp_server.start();
        tcp_server.start();
        cloud_server.start();

        running = true;
    }
    public void stop() {
        if (!running)
            return;
        BOut.trace("BmCommunicator::stop");

        try {
            udp_server.stop();
            tcp_server.stop();
            cloud_server.stop();
        } catch (Exception ex) {
            BOut.error("BmCommunicator::stop - " + ex);
        }

        udp_server = null;
        tcp_server = null;
        cloud_server = null;

        running = false;
    }

    //
    // sending data from master to client
    //
    public void sendCommand(String str) {
        tcp_server.sendCommand(str);
        cloud_server.sendCommand(str);
    }

    public void sendAudio(byte[] data, int pos, int len) {
        tcp_server.sendAudio(data, pos, len);
        cloud_server.sendAudio(data, pos, len);
    }

    public void sendImage(byte[] data, int pos, int len) {
        tcp_server.sendImage(data, pos, len);
        cloud_server.sendAudio(data, pos, len);
    }

    @Override
    public void onBmTcpServerCommandReceived(BmTcpClient c, String cmd) {
        callbacks.onBmCommunicatorCommandReceived(c, cmd);
    }

    @Override
    public void onBmTcpServerAudioReceived(BmTcpClient c, byte[] data, int pos, int len) {
        callbacks.onBmCommunicatorAudioReceived(c, data, pos, len);
    }

    @Override
    public void onBmCloudServerCommandReceived(int cid, String cmd) {
        callbacks.onBmCommunicatorCommandReceived(null, cmd);
    }

    @Override
    public void onBmCloudServerAudioReceived(int cid, byte[] data, int pos, int len) {
        callbacks.onBmCommunicatorAudioReceived(null, data, pos, len);
    }
}

