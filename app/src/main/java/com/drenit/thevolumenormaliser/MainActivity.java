package com.drenit.thevolumenormaliser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
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
import java.util.prefs.Preferences;


public class MainActivity extends ActionBarActivity {

    private static final int ANDROID_KITKAT_SDK = 19;

    SparseArray<String> irData;

    /* constants */
    private static final String LOG_TAG = "MainActivity";
    private static final int POLL_INTERVAL = 500;
    private static final int PRESET_TOLERANCE_PERCENTAGE = 50;

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

        irData = new SparseArray<>();
        irData.put(
                R.id.buttonPower,
//  Samsung
                "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0040 0015 0015 0015 003f 0015 003f 0015 003f 0015 003f 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e");
// Logik                hex2dec("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0699 015B 0057 0016 0EA3"));
        irData.put(
                R.id.buttonMute,
//Samsung
                "0000 006c 0022 0003 00ab 00aa 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 003f 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0714 00ab 00aa 0015 0015 0015 0e91");
// Logik                hex2dec("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0699 015B 0057 0016 0EA3"));
        irData.put(
                R.id.buttonChUp,
// Samsung
                "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 0015 0015 003f 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 003f 0015 003f 0015 0015 0015 0040 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e");
// Logik               hex2dec("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0699 015B 0057 0016 0EA3"));

        irData.put(
                R.id.buttonChDown,
// Samsung
                "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 003f 0015 0015 0015 003f 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e");
//Logik               hex2dec("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0699 015B 0057 0016 0EA3"));

        irData.put(
                R.id.buttonVolUp,
// Samsung
                "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e");
//Logik                hex2dec("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0699 015B 0057 0016 0EA3"));

        irData.put(
                  R.id.buttonVolDown,
// Samsung
                  "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 0015 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 003f 0015 003f 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e");
//Logik                hex2dec("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0041 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0041 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0041 0016 0699 015B 0057 0016 0EA3"));


        mActivityLed = (ImageView) findViewById(R.id.activity_led);
        mThresholdView = (TextView) findViewById(R.id.threshold);


        mSensor = new SoundMeter();
        mDisplay = (SoundLevelView) findViewById(R.id.volume);

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
            start();
        }
    };

    View.OnClickListener mClearListener = new View.OnClickListener() {
        public void onClick(View v) {
            mThreshold = 0;
            mThresholdSet = false;
            mThresholdView.setText("");
            stop();
            Log.i(LOG_TAG, "RESET..");
        }
    };

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
            data = count2duration(data);
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

    @Override
    public void onResume() {
        super.onResume();
        readApplicationPreferences();
        mDisplay.setLevel(0, mThreshold);
        if (mAutoResume) {
            start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stop();
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
                Intent prefs = new Intent(this, Preferences.class);
                startActivity(prefs);
                break;
            case R.id.start_stop:
                if (!mRunning) {

                    mAutoResume = true;
                    mRunning = true;
                    start();
                } else {
                    mAutoResume = false;
                    mRunning = false;
                    stop();
                }
                break;
            case R.id.test:
                start();
                break;
            case R.id.panic:
//                callForHelp();
                break;
            case R.id.help:
                Intent myIntent = new Intent();
                myIntent.setClass(this, HelpActivity.class);
                startActivity(myIntent);
        }
        return true;

    }

    public void receive(String cmd) {
        if (cmd == "start" & !mRunning) {
        } else if (cmd == "stop" & mRunning) {
            mAutoResume = false;
            mRunning = false;
            stop();
        }
    }

    private void start() {
        mTickCount = 0;
        lastAmpValue = 0;
        breachTick = 0;
        try {
            mSensor.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //@todo this should obviously be done in a non blocking thread
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
                if(count++ == 10){
                    notSet = false;
                }
            }
            // Get average amplitude over "count - 1" seconds (count will have been incremented post operation above)
            mThreshold = summedAmplitude / (count - 1);
            // Show threshold in the view
            mThresholdView.setText(String.valueOf(mThreshold));
            Log.i(LOG_TAG, String.format("Max Amp: [%f] ::: Av Amp: [%f]", mThresholdMax, mThreshold));

        }
        setActivityLed(true);
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        mHandler.postDelayed(mPollTask, POLL_INTERVAL);
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

            /*if (amp > mThresholdMax) {
                lastAmpValue++;
                Log.i(LOG_TAG, String.format("High %d :: Amp: %f", lastAmpValue, amp));
                if (lastAmpValue > 5){
                    // Turn down the TV
                    Log.i(LOG_TAG, "Turn down the TV");
                    mThresholdView.setText("Turn down the TV");
                    irSend(R.id.buttonVolDown);
                    // Reset the count
                    lastAmpValue = 0;
                }
            }*/

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
        mHandler.removeCallbacks(mSleepTask);
        mHandler.removeCallbacks(mPollTask);
        mSensor.stop();
        mDisplay.setLevel(0,0);
        updateDisplay(0.0);
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
        mThreshold = Integer.parseInt(prefs.getString("threshold", null));
        Log.i(LOG_TAG, "threshold=" + mThreshold);
        mPollDelay = Integer.parseInt(prefs.getString("sleep", null));
        Log.i(LOG_TAG, "sleep=" + mPollDelay);
    }

    private void updateDisplay(double signalEMA) {
        mDisplay.setLevel((int)signalEMA, mThreshold);
    }

    private void setActivityLed(boolean on) {
        mActivityLed.setVisibility( on ? View.VISIBLE : View.INVISIBLE);
    }

    private Runnable mSleepTask = new Runnable() {
        public void run() {
            start();
        }
    };

}
