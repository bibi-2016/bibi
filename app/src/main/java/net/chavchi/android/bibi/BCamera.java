package net.chavchi.android.bibi;

import android.content.Context;
import android.graphics.ImageFormat;
    import android.hardware.Camera;
import android.view.SurfaceView;

interface BCameraEvents {
    void onImageCaptured(BImage img);
}

public class BCamera {
    private volatile boolean running = false;

    // input from ctor
    private BCameraEvents callbacks = null;
    private int width;
    private int height;

    private int camera_id;
    private int camera_orientation;
    private Camera camera = null;
    private Context context;

    // actual image width and height ... depending on camera orientation and capabilities
    private int image_width;
    private int image_height;

    // image object that is filled in worker thread and returned in on image clapture event
    private BImage image = new BImage();
    private byte[] image_data = null;

    // rotate nv21 encoded image
    //    - input is input data
    //    - width, height are dimensionas of input data
    //    - rotation is the orientation of camera (according to portrait position of the phone)
    //    - output is the rotated data ! keep in mind that if the rotation is 90 or 270, the
    //      image dimensios are swapped !
    public static void rotateNV21(byte[] input, int width, int height, int rotation, byte[] output) {
        // size of Y data
        final int y_size = width * height;

        // input image dimensions
        final int wi = width;
        final int hi = height;

        // output image dimensions
        final int wo = (rotation == 90 || rotation == 270) ? height : width;
        final int ho = (rotation == 90 || rotation == 270) ? width : height;

        // swap xi and yi offsets
        final int xi_off = (wi - ho) >> 1;
        final int yi_off = ((wo + hi) >> 1) - 1;

        // if rotating square for
        //           0 degress -> ( x,  y)
        //          90 degress -> ( y, -x) => swap
        //         180 degress -> (-x, -y) => flip
        //         270 degress -> (-y,  x) => swap && flip
        //  (x,y) -> (-x,-y) => flip
        //  (x,y) -> (y, -x) => swap
        final boolean swap = (rotation == 90 || rotation == 270);
        final boolean flip = (rotation == 180 || rotation == 270);

        for (int yo = 0; yo < ho; yo++) {
            for (int xo = 0; xo < wo; xo++) {

                int xi = xo, yi = yo; // input x,y coordinate
                if (swap) {
                    xi = ( yo + xi_off);
                    yi = (-xo + yi_off);
                }
                if (flip) {
                    xi = wi - xi - 1;
                    yi = hi - yi - 1;
                }

                int Yo = yo * wo + xo; // location of out Y(x,y) pixel
                int Yi = yi * wi + xi; // location of input Y(x,y) pixel
                output[Yo] = input[Yi]; // copy Y channel

                if ((xo & 1) == 0 && (yo & 1) == 0) { //have u,v pixel to
                    int Uo = y_size + (yo >> 1) * wo + xo;

                    int Ui = y_size + (yi >> 1) * wi + xi;
                    if (flip)
                        Ui -= 1;

                    output[Uo + 0] = input[Ui + 0]; // copy U channel
                    output[Uo + 1] = input[Ui + 1]; // copy V channel
                }
            }
        }
    }

    private Camera.PreviewCallback worker = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Size previewSize = camera.getParameters().getPreviewSize();

            final int camera_width = previewSize.width;
            final int camera_height = previewSize.height;

            //BOut.println("got imagate ... " + camera_width + "x" + camera_height);
            if (image_data == null || image_data.length < data.length)
                image_data = new byte[data.length];

            rotateNV21(data, camera_width, camera_height, camera_orientation, image_data);
            image.set_nv21_data(image_data, image_width, image_height);

            // tell the world that we have new imgae
            callbacks.onImageCaptured(image);
        }
    };



    BCamera(int cid, int width, int height, BCameraEvents e, Context c) {
        callbacks = e;
        context = c;

        camera_id = cid;
        this.width = width;
        this.height = height;
    }


    public void start() {
        if (running)
            return;

        BOut.trace("BCamera::start");
        try {
            if (android.os.Build.VERSION.SDK_INT >= 9) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(camera_id, info);
                camera_orientation = info.orientation;

                camera = Camera.open(camera_id);
            } else {
                camera_orientation = 90;

                camera = Camera.open();
            }

            switch (camera_orientation) {
                case 0:
                case 180:
                    image_width = width;
                    image_height = height;
                    break;
                case 90:
                case 270:
                    image_width = height;
                    image_height = width;
                    break;
            }

            // get and set the camera parameters
            Camera.Parameters param = camera.getParameters();

            param.setPreviewSize(width, height);
            // to be sure set the format to default NV21
            param.setPreviewFormat(ImageFormat.NV21);
            // set parameters and worker callback
            camera.setParameters(param);
            camera.setPreviewCallback(worker);

            // phony display, so that previwecallback will work on all phones
            SurfaceView view = new SurfaceView(context);
            camera.setPreviewDisplay(view.getHolder());
        } catch (Exception ex) {
            BOut.println("BCamera::init - " + ex);
        }

        // camera is initialized ... start receiving preview
        camera.startPreview();

        running = true;
    }

    public void stop() throws Exception {
        if (!running)
            return;

        BOut.trace("BCamera::stop");

        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;

        running = false;
    }
}
