package tokyo.day.hack.music.com.myunic;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.util.List;

/**
 * Created by taku on 15/07/04.
 */
public class ApplicationHelper {
    public static int getCameraDisplayOrientation(Activity act, int nCameraID){
        if(Build.VERSION.SDK_INT >= 9){
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(nCameraID, info);
            int rotation = act.getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                //portate:縦向き
                case Surface.ROTATION_0: degrees = 0; break;
                //landscape:横向き
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            int result;
            //Camera.CameraInfo.CAMERA_FACING_FRONT:アウトカメラ
            //Camera.CameraInfo.CAMERA_FACING_BACK:インカメラ

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            return result;
        }
        return 90;
    }

    public static Bitmap bitmapRotate(Bitmap bmp, int orientation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int height = bmpOriginal.getHeight();
        int width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        bmpOriginal.recycle();
        bmpOriginal = null;
        return bmpGrayscale;
    }

    public interface DecodeFinishListener{
        public int onDecode(int pixel, int wIndex, int hIndex);
    }

    //YUV420 to BMP
    public static int[] decodeYUV420SP(byte[] yuv420sp, int width, int height, DecodeFinishListener listener) {
        int[] rgb = new int[(width * height)];
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                int pixel = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                int p = listener.onDecode(pixel, i, j);
                rgb[yp] = p;
            }
        }
        return rgb;
    }
}
