package net.chavchi.android.bibi;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.provider.MediaStore;
import android.view.View;


public class BImageView extends View {
    private Bitmap bitmap = null;
    private double volume = 0;

    private int view_width, view_height;
    private Rect image_rect;
    private Rect volume_rect;
    private Paint meshPaint, movementPaint, volumePaint;
    private Paint paint;

    private float zone_width;
    private float zone_heigh;
    private VideoStats.Stats zone_movement;

    private Context contex;

    BImageView(Context c) {
        super(c);

        contex = c;

        //App.log("BImageView() >");
        meshPaint = new Paint(0);
        meshPaint.setColor(0xffff0000);

        movementPaint = new Paint(0);
        movementPaint.setColor(0x8000ff00);

        volumePaint = new Paint(0);
        volumePaint.setColor(0xff0000ff);

        paint = new Paint();
    }

    private void update() {
        ((Activity)contex).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }


    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //App.log("BImageView.onSizeChanged() >");

        view_width = w;
        view_height = h;

        final int img_width = (int)(view_width * 0.80);
        final int img_height = w > h ? (int)(img_width * 3 / 4) : (img_width * 4 / 3);

        final int img_posx = (view_width - img_width) / 2;
        final int img_posy = img_posx;

        image_rect = new Rect(img_posx, img_posy, img_posx + img_width, img_posy + img_height);
        volume_rect = new Rect(image_rect.left, image_rect.bottom + 5, image_rect.right, image_rect.bottom + 15);
        zone_width = image_rect.width() / (float)VideoStats.nzx;
        zone_heigh = image_rect.height() / (float)VideoStats.nzy;
    }

    public void updateImage(Bitmap bit) {
        bitmap = bit;
        zone_movement = null;

        update();
    }

    public void updateImage(BImage img, VideoStats.Stats movement) {
        bitmap = img.getAsBitmap();
        zone_movement = movement;

        update();
    }

    public void updateVolume(double vol) {
        volume = vol;
        update();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, image_rect, null);

            if (zone_movement != null) {
                float x = image_rect.left;
                for (int i = 0; i <= VideoStats.nzx; i++, x += zone_width) {
                    canvas.drawLine(x, image_rect.top, x, image_rect.bottom, meshPaint);
                }

                float y = image_rect.top;
                for (int i = 0; i <= VideoStats.nzy; i++, y += zone_heigh) {
                    canvas.drawLine(image_rect.left, y, image_rect.right, y, meshPaint);
                }

                for (int j = 0; j < VideoStats.nzy; j++) {
                    for (int i = 0; i < VideoStats.nzx; i++) {
                        if (zone_movement.zone[j][i] == 1.0) {
                            final float zy = image_rect.top + j * zone_heigh;
                            final float zx = image_rect.left + i * zone_width;
                            canvas.drawRect(zx, zy, zx + 10, zy + 10, movementPaint);
                        }
                    }
                }
            }
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            canvas.drawRect(image_rect, paint);
            canvas.drawLine(image_rect.left, image_rect.top, image_rect.right, image_rect.bottom, paint);
            canvas.drawLine(image_rect.right, image_rect.top, image_rect.left, image_rect.bottom, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLUE);
        canvas.drawRect(volume_rect.left, volume_rect.top, volume_rect.left + (float)(volume_rect.width() * Math.min(volume, 1.0)), volume_rect.bottom, paint);
    }
}
