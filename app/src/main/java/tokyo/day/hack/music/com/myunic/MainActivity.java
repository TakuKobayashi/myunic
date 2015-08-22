package tokyo.day.hack.music.com.myunic;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {

    HeartRateScanCamera mHeartRateScanCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView bpmLable = (TextView) findViewById(R.id.bpmLabelText);
        bpmLable.setText(getString(R.string.bpmLabel, 0));

        Button heartScanButton = (Button) findViewById(R.id.HeartRateButton);
        heartScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHeartRateScanCamera.scanStart();
            }
        });

        mHeartRateScanCamera = new HeartRateScanCamera(this);
        mHeartRateScanCamera.setScanCallback(new HeartRateScanCamera.HeartScanCallback() {
            @Override
            public void onBeat(int bpm, long beatSpan) {
                TextView bpmLable = (TextView) findViewById(R.id.bpmLabelText);
                bpmLable.setText(getString(R.string.bpmLabel, bpm));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHeartRateScanCamera.releaseCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
