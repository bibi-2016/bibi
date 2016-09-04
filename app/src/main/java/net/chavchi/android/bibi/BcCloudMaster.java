package net.chavchi.android.bibi;

interface BcCloudMasterEvents extends BcMasterEvents {
    void onBcCloudMasterCommandReceived(BcCloudMaster m, String cmd);
    void onBcCloudMasterAudioReceived(BcCloudMaster m, byte[] data, int pos, int len);
    void onBcCloudMasterImageReceived(BcCloudMaster m, byte[] data, int pos, int len);
}

public class BcCloudMaster extends BcMaster {
    public BcCloudMaster(String name, String full_name, BcCloudMasterEvents e) {
        super(name, full_name, e);
    }
}
