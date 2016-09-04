package net.chavchi.android.bibi;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

//
//
// Main class for network communication
//
//
interface BcCommunicatorEvents {
    void onBcCommunicatorMastersChanged();

    void onBcCommunicatorCommandReceived(BcMaster m, String cmd);
    void onBcCommunicatorAudioReceived(BcMaster m, byte[] data, int pos, int len);
    void onBcCommunicatorImageReceived(BcMaster m, byte[] data, int pos, int len);
}

public class BcCommunicator implements BcUdpClientEvents, BcTcpMasterEvents, BcCloudServerEvents {
    private volatile boolean running = false;

    // input from ctor
    private BcCommunicatorEvents callbacks = null;
    private int port = 0;

    private BcUdpClient udp_client = null;
    private BcCloudServer cloud_server = null;

    private static final Object masters_lock = new Object();
    public List<BcTcpMaster> tcp_masters = new ArrayList<BcTcpMaster>();
    public List<BcCloudMaster> cloud_masters = new ArrayList<BcCloudMaster>();

    private Thread master_keeper_thread = null;

    // will loop over the workers and remove the ones that have not respond to udp search within last 3 seraches
    private Runnable worker = new Runnable() {
        @Override
        public void run() {
            int cnt = 0;

            try {
                while (running == true) {
                    Thread.sleep(100);
                    cnt++;

                    if (cnt > 100) { // around 10seconds passed
                        cnt = 0;

                        boolean list_changed = false;
                        long millis = System.currentTimeMillis();
                        synchronized (masters_lock) {

                            for (Iterator<BcTcpMaster> iterator = tcp_masters.iterator(); iterator.hasNext();) {
                                BcTcpMaster master = iterator.next();
                                if (master.getLastSeen(millis) > Bibi.cfg.udp_search_timeout * 3) { // did not see master for a while now ...
                                    master.close();
                                    iterator.remove();
                                    list_changed = true;
                                }
                            }
                        }

                        if (list_changed) {
                            callbacks.onBcCommunicatorMastersChanged();
                        }
                    }
                }
            } catch (Exception ex) {}
        }
    };


    BcCommunicator(int port, BcCommunicatorEvents e) {
        callbacks = e;
        this.port = port;
    }

    public void start() {
        if (running)
            return;
        BOut.trace("BcCommunicator::start");

        udp_client = new BcUdpClient(port, this);
        cloud_server = new BcCloudServer(this);

        udp_client.start();
        cloud_server.start();

        running = true;

        master_keeper_thread = new Thread(worker);
        master_keeper_thread.start();
    }

    public void stop() {
        if (!running)
            return;
        BOut.trace("BcCommunicator::stop");

        running = false;

        try {
            udp_client.stop();
            cloud_server.stop();

            master_keeper_thread.join();

            synchronized (masters_lock) {
                for (int i = 0; i < tcp_masters.size(); i++) {
                    tcp_masters.get(i).close();
                }
            }
        } catch (Exception ex) {
            BOut.println("BcCommunicator::stop - " + ex);
        }

        udp_client = null;
        cloud_server = null;
        master_keeper_thread = null;
    }

    public BcMaster getMaster(String full_name) {
        synchronized (masters_lock) {
            for (int i = 0; i < tcp_masters.size(); i++) {
                if (tcp_masters.get(i).full_name.equals(full_name)) {
                    return tcp_masters.get(i);
                }
            }
            for (int i = 0; i < cloud_masters.size(); i++) {
                if (cloud_masters.get(i).full_name.equals(full_name)) {
                    return cloud_masters.get(i);
                }
            }
        }

        return null;
    }

    //
    // BcUdpClient interface implementation
    //
    @Override
    public void onBcUdpClientMasterFound(String name, InetAddress address) {
        final String full_name = name + "@" + address.getHostAddress();
        BOut.trace("BcCommunicator::onBcUdpClientMasterFound(" + full_name + ")");
        synchronized (masters_lock) {
            long millis = System.currentTimeMillis();
            for (int i = 0; i < tcp_masters.size(); i++) {
                if (tcp_masters.get(i).full_name.equals(full_name)) { // master already connected
                    tcp_masters.get(i).setSeenAt(millis);
                    return;
                }
            }
        }

        new BcTcpMaster(name, full_name, address, port, this);
    }

    //
    // BcTcpMaster interface implementation
    //
    @Override
    public void addBcTcpMaster(BcTcpMaster m) {
        BOut.trace("BcCommunicator::addTcpMaster " + m.full_name);
        synchronized (masters_lock) {
            tcp_masters.add(m);
        }

        callbacks.onBcCommunicatorMastersChanged();
    }

    @Override
    public void removeBcTcpMaster(BcTcpMaster m) {
        BOut.trace("BcCommunicator::removeTcpMaster " + m.full_name);

        synchronized (masters_lock) {
            tcp_masters.remove(m);
        }

        callbacks.onBcCommunicatorMastersChanged();
    }

    @Override
    public void onBcTcpMasterCommandReceived(BcTcpMaster m, String cmd) {
        callbacks.onBcCommunicatorCommandReceived(m, cmd);
    }

    @Override
    public void onBcTcpMasterAudioReceived(BcTcpMaster m, byte[] data, int pos, int len) {
        callbacks.onBcCommunicatorAudioReceived(m, data, pos, len);
    }

    @Override
    public void onBcTcpMasterImageReceived(BcTcpMaster m, byte[] data, int pos, int len) {
        callbacks.onBcCommunicatorImageReceived(m, data, pos, len);
    }

    @Override
    public void onBcCloudServerCommandReceived(int cid, String cmd) {

    }

    @Override
    public void onBcCloudServerAudioReceived(int cid, byte[] data, int pos, int len) {

    }

    @Override
    public void onBcCloudServerImageReceived(int cid, byte[] data, int pos, int len) {

    }

    @Override
    public void onBcMasterEventsChange() {
        callbacks.onBcCommunicatorMastersChanged();
    }
}
