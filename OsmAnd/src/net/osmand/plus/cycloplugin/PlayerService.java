package net.osmand.plus.cycloplugin;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import net.osmand.aidlapi.IOsmAndAidlInterface;
import net.osmand.aidlapi.lock.ToggleLockParams;
import net.osmand.aidlapi.navigation.StopNavigationParams;
import net.osmand.plus.R;

public class PlayerService extends Service {
    public static final String TAG = "MPS";
    private MediaSessionCompat mediaSession;
    private IOsmAndAidlInterface mIOsmAndAidlInterface;
    private boolean running=false;






    private boolean bindOSMService() {
        if (mIOsmAndAidlInterface == null) {
            Intent intent = new Intent("net.osmand.aidl.OsmandAidlServiceV2");
            intent.setPackage("net.osmand");
            boolean res = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (res) {
                Toast.makeText(this, "OsmAnd service bind", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(this, "OsmAnd service NOT bind", Toast.LENGTH_SHORT).show();

                return false;
            }
        } else {
            return true;
        }
    }
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service);
        }
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mIOsmAndAidlInterface = null;
        }
    };
    private final MediaSessionCompat.Callback mMediaSessionCallback
            = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

            /*


             */
            final String intentAction = mediaButtonEvent.getAction();

            if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                final KeyEvent event = mediaButtonEvent.getParcelableExtra(
                        Intent.EXTRA_KEY_EVENT);
                if (event == null) {
                    return super.onMediaButtonEvent(mediaButtonEvent);
                }
                final int keycode = event.getKeyCode();
                final int action = event.getAction();
                if (event.getRepeatCount() == 0 && action == KeyEvent.ACTION_DOWN) {
                    switch (keycode) {
                        // Do what you want in here
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            if (mIOsmAndAidlInterface != null) {
                                try {
                                    mIOsmAndAidlInterface.toggleLock(new ToggleLockParams());
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                          //  MainActivity.showText2("KEYCODE_MEDIA_PLAY_PAUSE");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            break;
                    }
                   startService(new Intent(getApplicationContext(), PlayerService.class));
                    return true;
                }
            }
            return false;
        }
    };
    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.d("Handlers", "Called on main thread");
            final MediaPlayer mMediaPlayer;
            mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silent_sound);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mMediaPlayer.release();
                }
            });
            mMediaPlayer.start();
            startService(new Intent(getApplicationContext(), PlayerService.class));
            // Repeat this the same runnable code block again another 2 seconds
            if(running)
                handler.postDelayed(runnableCode, 10000);
        }
    };
// Start the initial runnable task by posting through the handler
    @Override
    public void onCreate() {
        super.onCreate();
        running=true;
        bindOSMService();
        ComponentName receiver = new ComponentName(getPackageName(), RemoteReceiver.class.getName());
        mediaSession = new MediaSessionCompat(this, "PlayerService", receiver, null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build());
        final MediaPlayer mMediaPlayer;
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silent_sound);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mMediaPlayer.release();
            }
        });
        mMediaPlayer.start();
        handler.post(runnableCode);

        mediaSession.setCallback(mMediaSessionCallback);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                // Ignore
                Log.d("HEADSET","focusChange=" + focusChange);
            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            Log.d("HEADSET","mediaSession set PAUSED state");
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build());
        } else {
            Log.d("HEADSET","mediaSession set PLAYING state");
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build());
        }
        return START_STICKY; // super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent("net.osmand.aidl.OsmandAidlServiceV2");
        unbindService(mConnection);

        mediaSession.release();
        running=false;
    }
}