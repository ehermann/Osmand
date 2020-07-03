package net.osmand.plus.cycloplugin;

import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;

import net.osmand.plus.activities.MapActivity;
/*
  click down: launch timer.
        Timer check if no up long
  click up get lock stop timer. as before.

 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class HeadsetGesturesPlugin extends OsmandPlugin {
    private int d = 0;
    private long lastReleaseTime=Long.MAX_VALUE;
    private long lastPressTime=Long.MAX_VALUE;

    private long maxDelay=0;

    private static final long FAST_PRESS_INTERVAL = 500; // in millis
    private static final long LONG_PRESS = 300; // in millis
    private static final long VERY_LONG_PRESS = 2000; // in millis

    private Handler handler;
    private Handler longPressHandler;
    private boolean is_down=false;
    abstract class CancelableRunnable implements Runnable {
        protected boolean removed = false;
        MapActivity mapActivity;
        void reset(MapActivity mActivity){mapActivity=mActivity;  removed = false;}
        public void remove () {
            removed = true;
        }
        public boolean isRemoved(){return removed;}
    };
    class KeyUpRunnable extends  CancelableRunnable{

        @Override
        public void run() {
            lock.lock();
            try {
                if(removed)
                    return;
                removed=true;

                if(lastReleaseTime-lastPressTime>LONG_PRESS)
                    d=0;
                headsetButton(mapActivity,d);

                // single click *******************************
                //Toast.makeText(mapActivity, "single click! "+d, Toast.LENGTH_SHORT).show();



                d = 0;
                maxDelay=0;
                lastReleaseTime=Long.MAX_VALUE;
            } finally {
                lock.unlock();
            }
        }
    };
    class KeyDownRunnable extends  CancelableRunnable{

        @Override
        public void run() {
            lock.lock();
            try {
                if(removed)
                    return;
                removed=true;

                headsetButton(mapActivity,-1);

                // single click *******************************
                //Toast.makeText(mapActivity, "Very long clic! "+d, Toast.LENGTH_SHORT).show();




            } finally {
                lock.unlock();
            }
        }
    };
    KeyUpRunnable keyUpRunnable;
    KeyDownRunnable keyDownRunnable;

    @Nullable
    ReentrantLock lock = new ReentrantLock();
    HeadsetGesturesPlugin(OsmandApplication app)
    {
        super(app);
        handler = new Handler();
        keyDownRunnable = new KeyDownRunnable();
        keyUpRunnable = new KeyUpRunnable();
        longPressHandler= new Handler();
    }
    @Override
    public boolean mapActivityKeyUp(final MapActivity mapActivity, int keyCode) {
        return keyUp(mapActivity,keyCode);
    }
    @Override
    public boolean mapActivityKeyDown(final MapActivity mapActivity, int keyCode) {
        lock.lock();
    try{
        if(is_down)
            return true;
        is_down=true;
        lastPressTime = System.currentTimeMillis();
        keyDownRunnable.reset(mapActivity);
        longPressHandler.postDelayed(keyDownRunnable, VERY_LONG_PRESS);
    } finally {
        lock.unlock();
    }
        return true;
    }
    public void headsetButton(MapActivity mapActivity,int clicks){}

private void removeRunnables()
{

        handler.removeCallbacks(keyUpRunnable);
    longPressHandler.removeCallbacks(keyDownRunnable);
    keyDownRunnable.remove();
    keyUpRunnable.remove();
}
    public Boolean keyUp(final MapActivity mapActivity, int keyCode)
    {
        Boolean returnValue=false;

        //switch (keyCode)
        {
        //    case KeyEvent.KEYCODE_HEADSETHOOK:
                lock.lock();
                try {
                    is_down=false;
                    lastReleaseTime = System.currentTimeMillis();

                    if(keyDownRunnable.isRemoved())
                    return true;

                removeRunnables();
                keyUpRunnable.reset(mapActivity);

                d++;

                    handler.postDelayed(keyUpRunnable, 800);
               } finally {
                     lock.unlock();
                }
          //      break;
        }
        return true;
    }

}
