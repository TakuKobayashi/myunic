package tokyo.day.hack.music.com.myunic;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.util.ArrayList;

/**
 * Created by akizuki on 2015/07/05.
 */
public class HeartRateScanCamera {
    private Activity mActivity;
    private Camera mCamera;
    private Camera.Size mPreviewSize;
    private HeartScanCallback mCallback;
    //指を当てている間は赤がほとんどをしめるため、指が当たっているかどうかの判定に使う
    private int redLightCounter = 0;
    // 明るい部分を数える
    private int lightFieldCount = 0;
    // 指が当たっている時、直前の明るい部分を記録して比較するのに使う
    private int prevSampling = 0;
    // 明るい部分が前回との差分により増減していくことで、1ループで1ビートとカウントできそう
    private int beatCounter = 0;
    //前回とのビートの時でBPがなんとなく出そう
    private long prevBeatSpan = 0;
    private int mSumLoopBeatCount;
    private ArrayList<Integer> bpmList;

    public HeartRateScanCamera(Activity act){
        mActivity = act;
        bpmList = new ArrayList<Integer>();
    }

    public void setScanCallback(HeartScanCallback callback){
        mCallback = callback;
    }

    public void scanStart(){
        mCamera = Camera.open();
        Camera.Parameters cp = mCamera.getParameters();

        mCamera.setDisplayOrientation(ApplicationHelper.getCameraDisplayOrientation(mActivity, 0));
        mPreviewSize = cp.getPreviewSize();
        cp.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(cp);

        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                lightFieldCount = 0;
                redLightCounter = 0;
                int[] rgb = ApplicationHelper.decodeYUV420SP(data, mPreviewSize.width, mPreviewSize.height, new ApplicationHelper.DecodeFinishListener() {
                    @Override
                    public int onDecode(int pixel, int wIndex, int hIndex) {
                        int A = Color.alpha(pixel);
                        int R = Color.red(pixel);
                        int G = Color.green(pixel);
                        int B = Color.blue(pixel);
                        int grayscale = (int) (0.299 * R + 0.587 * G + 0.114 * B);
                        if (R > 128) {
                            ++redLightCounter;
                        }
                        if (grayscale > 128) {
                            ++lightFieldCount;
                        }
                        return Color.argb(A, R, G, B);
                    }
                });
                //指が当たっていない
                if (redLightCounter < (mPreviewSize.width * mPreviewSize.height) * 0.9) {
                    prevSampling = 0;
                    beatCounter = 0;
                    prevBeatSpan = System.currentTimeMillis();
                    mSumLoopBeatCount = 0;
                    bpmList.clear();
                    return;
                }
                if (prevSampling == 0) {
                    prevSampling = lightFieldCount;
                    prevBeatSpan = System.currentTimeMillis();
                } else {
                    if (prevSampling < lightFieldCount) {
                        ++beatCounter;
                        if (beatCounter > 1) beatCounter = 1;
                    } else {
                        --beatCounter;
                        if (beatCounter < -1) beatCounter = -1;
                    }
                    if (beatCounter == 0) {
                        ++mSumLoopBeatCount;
                        if(mSumLoopBeatCount % 2 == 0){
                            long span = System.currentTimeMillis() - prevBeatSpan;
                            int bpm = (int) ((float) 1 * 1000 * 60 / span);
                            prevBeatSpan = System.currentTimeMillis();
                            bpmList.add(bpm);
                            int sum = 0;
                            for(int i = 0;i < bpmList.size();++i) {
                                sum += bpmList.get(i);
                            }
                            if(mCallback != null) mCallback.onBeat(sum / bpmList.size(), span);
                        }

                    }
                    prevSampling = lightFieldCount;
                }
            }
        });
        mCamera.startPreview();
    }

    public void releaseCamera() {
        if (mCamera != null){
            Camera.Parameters cp = mCamera.getParameters();
            cp.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(cp);
            mCamera.cancelAutoFocus();
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        };
    }

    public interface HeartScanCallback{
        public void onBeat(int bpm, long beatSpan);
    }
}
