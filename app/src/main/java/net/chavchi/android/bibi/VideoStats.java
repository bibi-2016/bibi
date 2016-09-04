package net.chavchi.android.bibi;

import java.util.Vector;

public class VideoStats {
    final static int nzx = 16;
    final static int nzy = 16;

    public Stats movement = null;

    class Stats {
        float[][] zone = new float[nzy][nzx];

        Stats clearZones() { // set zone array to 0
            for (int j = 0; j < nzy; j++) {
                for (int i = 0; i < nzx; i++) {
                    zone[j][i] = 0;
                }
            }

            return this;
        }

        public String toString() {
            String str = "";

            for (int i = 0; i < nzx; i++) {
                for (int j = 0; j < nzy; j++) {
                    str += "[" + i + "," + j + "]=" + zone[j][i] + " ";
                }
                str += "\n";
            }

            return str;
        }

        double sumZones() {
            float sum = 0;
            for (int j = 0; j < nzy; j++) {
                for (int i = 0; i < nzx; i++) {
                    sum += zone[j][i];
                }
            }

            return sum;
        }

        double getMaxZoneValue() {
            float max = Float.MIN_VALUE;

            for (int j = 0; j < nzy; j++) {
                for (int i = 0; i < nzx; i++) {
                    if (zone[j][i] > max)
                        max = zone[j][i];
                }
            }

            return max;
        }

        Stats normalizeZones() {
            double max = getMaxZoneValue();

            for (int j = 0; j < nzy; j++) {
                for (int i = 0; i < nzx; i++) {
                    zone[j][i] /= max;
                }
            }

            return this;
        }

        Stats binarizeZones(double th) {
            for (int j = 0; j < nzy; j++) {
                for (int i = 0; i < nzx; i++) {
                    zone[j][i] = zone[j][i] > th ? 1.0f : 0.0f;
                }
            }

            return this;
        }
    }

    class AvgStats extends Stats {
        AvgStats(BImage image) {
            final int zw = image.width / nzx;
            final int zh = image.height / nzy;
            final int zpc = zw * zh;

            clearZones();

            // process pixels
            int inp = 0;
            for (int j = 0; j < image.height; j++) {
                for (int i = 0; i < image.width; i++, inp++) {
                    final int y = j / zh; // / nzy
                    final int x = i / zw; // / nzx

                    zone[y][x] += (image.nv21_data[inp] & 0xff);
                }
            }

            // finish zone calculation
            for (int j = 0; j < nzy; j++) {
                for (int i = 0; i < nzx; i++) {
                    zone[j][i] /= zpc;
                }
            }
        }
    }

    Stats getAbsDiffStats(Stats a, Stats b) {
        Stats out = new Stats();

        for (int j = 0; j < nzy; j++) {
            for (int i = 0; i < nzx; i++) {
                out.zone[j][i] = Math.abs(a.zone[j][i] - b.zone[j][i]);
            }
        }

        return out;
    }

    private Vector avg_stats = new Vector();


    VideoStats() {
    }

    void processNewImage(BImage image) {
        // calculate luma average in the zones
        AvgStats curr_avg = new AvgStats(image);
        // normalize zones
        curr_avg.normalizeZones();
        // add avg stats to vector and trim it to 10 elements
        avg_stats.add(curr_avg);
        if (avg_stats.size() > 10) {
            avg_stats.remove(0);
        }
    }

    boolean haveMovement(double th) {
        // see if movement between last two frames
        if (avg_stats.size() < 2) { // not enough frames
            return false;
        }

        int curr = avg_stats.size() - 1;
        int prev = avg_stats.size() - 2;

        AvgStats curr_avg = (AvgStats)avg_stats.get(curr);
        AvgStats prev_avg = (AvgStats)avg_stats.get(prev);

        movement = getAbsDiffStats(prev_avg, curr_avg);
        movement.binarizeZones(th);

        if (movement.sumZones() > 0) {
            return true;
        }

        return false;
    }
}
