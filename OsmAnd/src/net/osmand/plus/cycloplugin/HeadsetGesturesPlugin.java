package net.osmand.plus.cycloplugin;

import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;

import net.osmand.plus.activities.MapActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class HeadsetGesturesPlugin extends OsmandPlugin {
    private int d = 0;
    private long lastPressTime=Long.MAX_VALUE;
    private long maxDelay=0;
    private static final long FAST_PRESS_INTERVAL = 500; // in millis
    private static final long SLOW_PRESS_INTERVAL = 1000; // in millis
    private Handler handler;
    abstract class CancelableRunnable implements Runnable {
        protected boolean removed = false;
        public void remove () {
            removed = true;
        }
    };
    @Nullable
    CancelableRunnable previousRunnable;
    ReentrantLock lock = new ReentrantLock();
    HeadsetGesturesPlugin(OsmandApplication app)
    {
        super(app);
        handler = new Handler();
    }
    @Override
    public boolean mapActivityKeyUp(final MapActivity mapActivity, int keyCode) {
        return keyUp(mapActivity,keyCode);
    }
    @Override
    public boolean mapActivityKeyDown(final MapActivity mapActivity, int keyCode) {
        return true;
    }
    public void headsetButton(MapActivity mapActivity,int clicks){}


    public Boolean keyUp(final MapActivity mapActivity, int keyCode)
    {
        Boolean returnValue=false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                CancelableRunnable r = new CancelableRunnable() {

                    @Override
                    public void run() {
                        lock.lock();
                        try {
                            if(removed)
                                return;
                            if(maxDelay>FAST_PRESS_INTERVAL)
                                d=0;
                            headsetButton(mapActivity,d);

                            // single click *******************************
                            Toast.makeText(mapActivity, "single click! "+d, Toast.LENGTH_SHORT).show();



                            d = 0;
                            maxDelay=0;
                            lastPressTime=Long.MAX_VALUE;
                        } finally {
                            lock.unlock();
                        }
                    }
                };
                lock.lock();
                try {
                    long pressTime = System.currentTimeMillis();
                    maxDelay=Math.max(maxDelay,pressTime-lastPressTime);
                    lastPressTime=pressTime;
                    d++;
                    if(previousRunnable!=null) {
                        handler.removeCallbacksAndMessages(previousRunnable);
                        previousRunnable.remove();
                    }
                    previousRunnable=r;
                } finally {
                    lock.unlock();
                }


                    handler.postDelayed(r, 800);

                break;
        }
        return true;
    }
}
