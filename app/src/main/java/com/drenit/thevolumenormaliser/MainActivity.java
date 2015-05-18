package com.drenit.thevolumenormaliser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {


    private SparseArray<String> irData;
    private static final List<Integer> BUTTON_ORDER = Arrays.asList(R.id.buttonPower, R.id.buttonMute, R.id.buttonChUp, R.id.buttonChDown, R.id.buttonVolUp, R.id.buttonVolDown);

    /* constants */
    private static final String LOG_TAG = "MainActivity";
    private static final int POLL_INTERVAL = 500;
    private static final int PRESET_TOLERANCE_PERCENTAGE = 50;
    private static final int ANDROID_KITKAT_SDK = 19;
    private static final int SAMPLE_TIME = 10;

    /** running state **/
    private boolean mAutoResume = false;
    private boolean mRunning = false;
    private int mTickCount = 0;
    private double lastAmpValue = 0;
    private int lastPolarity = 0;
    private int breachTick =0;

    private int mBreachCount =0;
    private int mLowCount =0;

    /** config state **/
    private boolean mThresholdSet;
    private double mThreshold;
    private double mThresholdMax;
    private int mPollDelay;
    private static AsyncTask mLevelReader;

    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler();

    /* References to view elements */
    private ImageView mActivityLed;
    private SoundLevelView mDisplay;

    private TextView mThresholdView;

    /* data source */
    private SoundMeter mSensor;

    ConsumerIrManager mCIR;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mCIR = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        Log.e(LOG_TAG, "mCIR.hasIrEmitter(): " + mCIR.hasIrEmitter());

        mActivityLed = (ImageView) findViewById(R.id.activity_led);
        mThresholdView = (TextView) findViewById(R.id.threshold);


        mSensor = new SoundMeter();


        findViewById(R.id.buttonLockLevel).setOnClickListener(mLockListener);
        findViewById(R.id.buttonClear).setOnClickListener(mClearListener);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "NoiseAlert");

    }

    View.OnClickListener mLockListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (!mCIR.hasIrEmitter()) {
                Log.e(LOG_TAG, "No IR Emitter found\n");
                return;
            }
            Log.i(LOG_TAG, "LOCKING VOLUME LEVEL..");
//            start();
            if(!mRunning){
                mLevelReader = new ReadCurrentLevel().execute();
            }
        }
    };

    View.OnClickListener mClearListener = new View.OnClickListener() {
        public void onClick(View v) {
            reset();
        }
    };

    void reset(){
        mThreshold = 0;
        mThresholdSet = false;
        mThresholdView.setText("");
        stop();
        if(mLevelReader != null)
            mLevelReader.cancel(false);
        Log.i(LOG_TAG, "RESET..");
    }


    @Override
    public void onResume() {
        super.onResume();
        readApplicationPreferences();
    }

    @Override
    public void onStop() {
        super.onStop();
        stop();
    }


    private Runnable mPollTask = new Runnable() {
        public void run() {

            double amp = mSensor.getAmplitude();
            Log.i(LOG_TAG, "\n\nThreshold: "+mThreshold);
            Log.i(LOG_TAG, "Current AMP: "+amp);
            updateDisplay(amp);
            // Show threshold in the view
            mThresholdView.setText(String.valueOf(mThreshold));

            // Check if % difference exceeds a preset tolerance
            double percentageDifference = ((amp - mThreshold)/mThreshold) * 100;
            Log.i(LOG_TAG, "Percentage Difference: ["+percentageDifference+"]");

            int thisPolarity = checkPolarity(percentageDifference);

            if(Math.abs(percentageDifference) > PRESET_TOLERANCE_PERCENTAGE){
                // If polarity has changed, reset breach count
                if(thisPolarity != lastPolarity) mBreachCount = 0;
                mBreachCount++;
                Log.i(LOG_TAG, "BreachCount: ["+mBreachCount+"]");

                if(mBreachCount > 5){
                    if(thisPolarity > 0){
                        Log.i(LOG_TAG, "HIGH! TURN DOWN!");
                        irSend(R.id.buttonVolDown);
                    }else{
                        Log.i(LOG_TAG, "LOW! TURN UP!");
                        irSend(R.id.buttonVolUp);
                    }
                    mBreachCount--;
                }
            }

            // Set lastPolarity
            lastPolarity = thisPolarity;

            mTickCount++;
            setActivityLed(mTickCount % 2 == 0);

            // Set last amp value to current
            lastAmpValue = amp;
            mHandler.postDelayed(mPollTask, POLL_INTERVAL);
        }
    };

    int checkPolarity(double value){
        if(value == 0) return 0;
        return value > 0 ? 1 : -1;
    }

    private void stop() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if(mHandler != null){
            mHandler.removeCallbacks(mSleepTask);
            mHandler.removeCallbacks(mPollTask);
        }
        mSensor.stop();
        if(mDisplay != null){
            mDisplay.setLevel(0,0);
            updateDisplay(0.0);
        }
        setActivityLed(false);
        mRunning = false;
    }

    private void sleep() {
        mSensor.stop();
        updateDisplay(0.0);
        setActivityLed(false);
        mHandler.postDelayed(mSleepTask, 1000*mPollDelay);
    }

    private void readApplicationPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String tvSelection = prefs.getString("tv_setting_selection", null);
        // Load the Array with the TV selection:
        String codes = getStringResourceByName(tvSelection);
        String[] codesArray = codes.split(":");
        // Clear the contents of the irData SparseArray
        irData = new SparseArray<>();
        // Now add the values from above
        for(int i = 0; i < BUTTON_ORDER.size(); i++){
            irData.put(BUTTON_ORDER.get(i), codesArray[i]);

        }
        Log.i(LOG_TAG, "chosen TV=" + tvSelection);
    }

    private String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "string", packageName);
        return getString(resId);
    }

    private void updateDisplay(double signalEMA) {
        mDisplay.setLevel((int)signalEMA, mThreshold);
    }

    private void setActivityLed(boolean on) {
        mActivityLed.setVisibility( on ? View.VISIBLE : View.INVISIBLE);
    }

    private Runnable mSleepTask = new Runnable() {
        public void run() {
//            start();
        }
    };

    private class ReadCurrentLevel extends AsyncTask<Void, Double, Double> {

        @Override
        protected void onPreExecute() {
            mThresholdView.setText("Sampling noise level...");
            // Setup level meter
            mDisplay = (SoundLevelView) findViewById(R.id.volume);
            // Setting the threshold to 10 will make the progress
            // indicator show green bars for ambiance level sampling
            mThreshold = 12;
            updateDisplay(0);
            mRunning = true;
        }

        @Override
        protected Double doInBackground(Void... params) {
            mTickCount = 0;
            lastAmpValue = 0;
            breachTick = 0;
            try {
                mSensor.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(!mThresholdSet){
                // Value not set, set threshold based on current volume
                boolean notSet = true;
                int count = 1;
                double summedAmplitude = 0;
                while (notSet){
                    double thisAmplitude = mSensor.getAmplitude();
                    summedAmplitude += thisAmplitude;
                    if(thisAmplitude > mThresholdMax)
                    {
                        mThresholdMax = thisAmplitude;
                        Log.i(LOG_TAG, String.format("New Max: %f", thisAmplitude));
                    }
                    try{
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Set to 10 seconds
                    Log.i(LOG_TAG, String.format("Setting level :: count: [%d] :: Sum: [%f] :: Amplitude: [%f]", count, summedAmplitude, thisAmplitude));
                    if(isCancelled() || count++ == SAMPLE_TIME){
                        notSet = false;
                    }
                    publishProgress(Double.valueOf(count));

                }
                // Evaluate sample time
                mThreshold = summedAmplitude / SAMPLE_TIME;
                // Show threshold in the view
                Log.i(LOG_TAG, String.format("Max Amp: [%f] ::: Av Amp: [%f]", mThresholdMax, mThreshold));
            }
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            return mThreshold;
        }

        @Override
        protected void onProgressUpdate(Double... progress) {
            // Update info
            updateDisplay(progress[0]);
        }

        @Override
        protected void onPostExecute(Double threshold) {
            setActivityLed(true);
            mThresholdView.setText(String.valueOf(threshold));
            mHandler.postDelayed(mPollTask, POLL_INTERVAL);
            mRunning = false;
        }

        @Override
        protected void onCancelled(Double threshold) {
            updateDisplay(0);
            mRunning = false;
        }
    }

    public void irSend(View view) {
        irSend(view.getId());
    }

    public void irSend(int irDataIndex) {

        if (!mCIR.hasIrEmitter()) {
            Log.e(LOG_TAG, "No IR Emitter found\n");
            return;
        }

        String data = irData.get(irDataIndex);
        int lastIdx = Build.VERSION.RELEASE.lastIndexOf(".");
        int VERSION_MR = Integer.valueOf(Build.VERSION.RELEASE.substring(lastIdx+1));
        if (VERSION_MR < 3) {
            // Before version of Android 4.4.2
            data = hex2dec(data);
        } else {
            // Later version of Android 4.4.3
            data = count2duration(hex2dec(data));
        }
        if (data != null) {
            String values[] = data.split(",");
            int[] pattern = new int[values.length-1];
            int frequency = Integer.parseInt(values[0]);

            //@todo Start index at 1??
            for (int i=0; i<pattern.length; i++){
                pattern[i] = Integer.parseInt(values[i+1]);
            }
            mCIR.transmit(frequency, pattern);
        }


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

        switch(item.getItemId()) {
            case R.id.settings:
                Log.i(LOG_TAG, "settings");
                Intent prefs = new Intent(this, SettingsActivity.class);
                startActivity(prefs);
                break;
            case R.id.help:
                Intent myIntent = new Intent();
                myIntent.setClass(this, HelpActivity.class);
                startActivity(myIntent);
        }
        return true;

    }

    protected String hex2dec(String irData) {
        List<String> list = new ArrayList<String>(Arrays.asList(irData
                .split(" ")));
        list.remove(0); // dummy
        int frequency = Integer.parseInt(list.remove(0), 16); // frequency
        list.remove(0); // seq1
        list.remove(0); // seq2

        for (int i = 0; i < list.size(); i++) {
            list.set(i, Integer.toString(Integer.parseInt(list.get(i), 16)));
        }

        frequency = (int) (1000000 / (frequency * 0.241246));
        list.add(0, Integer.toString(frequency));

        irData = "";
        for (String s : list) {
            irData += s + ",";
        }
        return irData;
    }

    protected String count2duration(String countPattern) {
        List<String> list = new ArrayList<String>(Arrays.asList(countPattern.split(" ")));
        int frequency = Integer.parseInt(list.get(0));
        int pulses = 1000000/frequency;
        int count;
        int duration;

        list.remove(0);

        for (int i = 0; i < list.size(); i++) {
            count = Integer.parseInt(list.get(i));
            duration = count * pulses;
            list.set(i, Integer.toString(duration));
        }

        String durationPattern = "";
        for (String s : list) {
            durationPattern += s + ",";
        }

        Log.d(LOG_TAG, "Frequency: " + frequency);
        Log.d(LOG_TAG, "Duration Pattern: " + durationPattern);

        return durationPattern;
    }

}
