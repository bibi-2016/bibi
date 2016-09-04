package net.chavchi.android.bibi;

interface BcMasterEvents {
    void onBcMasterEventsChange();
}

public class BcMaster implements BEventsEvents {
    protected volatile boolean running = false;
    private BcMasterEvents callbacks = null;

    enum Origin{
        Local, Cloud;
    }

    private Origin from;

    public final String name;        //local name     cloud name@ip
    public final String full_name;   //local name@ip  cloud name@ip

    public int ID = 0;
    public TcpCommunicator communicator = null;

    public String events_status;
    protected BEvents events = null;

    protected long last_seen_at = 0;

    public BcMaster(String name, String full_name, BcMasterEvents e) {
        callbacks = e;

        this.name = name;
        this.full_name = full_name;

        events = new BEvents(this);
        events.start();
    }

    public void start() {
        running = true;
        last_seen_at = System.currentTimeMillis();
    }

    public void stop() {
        running = false;

        events.stop();
        events = null;
    }

    public void setSeenAt(long m) {
        last_seen_at = m;
    }
    public long getLastSeen(long m) {
        return m - last_seen_at;
    }
    public boolean isAlive() {
        return running;
    }

    @Override
    public void onBEventsEventsChanged(String status) {
        events_status = status;
        callbacks.onBcMasterEventsChange();
    }

    public void sendAudio(byte[] data, int pos, int len) {
        communicator.setClientID(ID);
        communicator.sendAudio(data, pos, len);
    }
    public void sendCommand(String cmd) {
        communicator.setClientID(ID);
        communicator.sendCommand(cmd);
    }
}
