package net.chavchi.android.bibi;

import java.util.Timer;
import java.util.TimerTask;

class BEvent {
    public final String name;
    boolean triggered = false;
    public long triggeredAt = 0;

    BEvent(String n) {
        name = n;
    }
}

interface BEventsEvents {
    void onBEventsEventsChanged(String status);
}

public class BEvents {
    public static final int Motion = 0;
    public static final int Sound  = 1;
    public static final int Alarm = 2;

    private volatile boolean running = false;

    private BEventsEvents callbacks = null;

    private BEvent events[] = {new BEvent("Motion"), new BEvent("Sound"), new BEvent("Alarm")};
    private Timer timeout_timer;

    private TimerTask worker  = new TimerTask() {
        @Override
        public void run() {
            final long millis = System.currentTimeMillis();
            for (int i = 0; i < events.length; i++) {
                if (events[i].triggered && (millis - events[i].triggeredAt > Bibi.cfg.event_duration_timeout)) {
                    clearEvent(i);
                }
            }
        }
    };

    public BEvents(BEventsEvents e) {
        callbacks = e;
    }

    public void start() {
        if (running)
            return;

        timeout_timer = new Timer();
        timeout_timer.schedule(worker, 1000, 1000);

        running = true;
    }

    public void stop() {
        if (!running)
            return;

        timeout_timer.cancel();
        timeout_timer = null;

        running = false;
    }

    public boolean setEvent(int e) {
        final long millis = System.currentTimeMillis();

        if (events[e].triggered == false) { //first time
            events[e].triggered = true;
            events[e].triggeredAt = millis;
            // update recorder event status text
            updateEventsStatus();

            return true;
        }

        return false;
    }

    private void clearEvent(int e) {
        events[e].triggered = false;
        // update recorder event status text
        updateEventsStatus();
    }

    private void updateEventsStatus() {
        String msg = "";
        for (int i = 0; i < events.length; i++) {
            if (events[i].triggered) {
                msg += events[i].name + " ";
            }
        }

        callbacks.onBEventsEventsChanged(msg);
    }

    public void parse(String event) {
        if (event.equals("Motion")) {
            setEvent(Motion);
        } else if (event.equals("Sound")) {
            setEvent(Sound);
        } else if (event.equals("Alarm")) {
            setEvent(Alarm);
        } else {
            BOut.error("Unknown event: " + event);
        }
    }
}
