package net.chavchi.android.bibi;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import java.io.ByteArrayOutputStream;


public class BImage {
    public int width = 0;
    public int height = 0;

    public byte[] nv21_data = null; //YCbCr format with nv21 encoding
    public int nv21_data_size = 0;

    private int[] argb = null;

    // compress nv21 buffer to jpeg
    private byte[] nv21_to_jpeg(byte[] nv21, int width, int height) {
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);

        return baos.toByteArray();
    }

    // convert nv21 to argb
    private void nv21_to_rgb(byte[] yuv, int width, int height, int[] argb) {
        final int frameSize = width * height;

        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                argb[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    BImage() {
    }

    // copy nv21 data
    public void set_nv21_data(byte[] data, int w, int h) {
        if (nv21_data == null || nv21_data.length < data.length)
            nv21_data = new byte[data.length];

        System.arraycopy(data, 0, nv21_data, 0, data.length);
        nv21_data_size = data.length;

        width = w;
        height = h;
    }

    public final Bitmap getAsBitmap() {
        if (nv21_data == null)
            return null;

        if (argb == null || argb.length < width*height) {
            argb = new int[width * height];
        }
        nv21_to_rgb(nv21_data, width, height, argb);

        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
    }

    public final byte[] getAsJpeg() {
        if (nv21_data == null)
            return null;

        return nv21_to_jpeg(nv21_data, width, height);
    }
}