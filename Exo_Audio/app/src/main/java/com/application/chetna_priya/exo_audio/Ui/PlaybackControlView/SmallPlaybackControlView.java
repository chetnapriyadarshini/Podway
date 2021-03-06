package com.application.chetna_priya.exo_audio.ui.playbackcontrolview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.application.chetna_priya.exo_audio.R;

/**
 * Created by chetna_priya on 10/23/2016.
 */

public class SmallPlaybackControlView extends FrameLayout {

    private static final String TAG = SmallPlaybackControlView.class.getSimpleName();
    //private final MediaBrowserCompat mMediaBrowser;
    //  @BindView(R.id.play_pause) ImageButton playButton;
    public static final String CUSTOM_ACTION_SPEED_CHANGE = "custom_action_speed_changed";
    public static final String SPEED = "speed";
    //   private Context mContext;

    public SmallPlaybackControlView(Context context) {
        this(context, null);
    }

    public SmallPlaybackControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmallPlaybackControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //   this.mContext = context;
        //ComponentListener componentListener = new SmallPlaybackControlView.ComponentListener();

        LayoutInflater.from(context).inflate(R.layout.layout_current_audio, this);
      /*  ButterKnife.bind(this,view);

        playButton.setOnClickListener(componentListener);

        mMediaBrowser = new MediaBrowserCompat(mContext,
                new ComponentName(mContext, PodcastService.class), mConnectionCallback, null);

        mMediaBrowser.connect();*/
    }
/*
    private void updatePlayPauseButton() {
        MediaControllerCompat mediaControllerCompat= ((FragmentActivity)mContext).getSupportMediaController();

        PlaybackStateCompat state = mediaControllerCompat.getPlaybackState();
        boolean playing = state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING;
        Log.d(TAG, "IN UPDATE PLAY PAUSE BUTTON, PLAYER IS PLAYING "+playing);
        String contentDescription = getResources().getString(
                playing ? R.string.exo_controls_pause_description : R.string.exo_controls_play_description);
        playButton.setContentDescription(contentDescription);
        playButton.setImageResource(
                playing ? R.drawable.exo_controls_pause : R.drawable.exo_controls_play);

    }*/

    /*@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (player == null || event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                player.setPlayWhenReady(!player.getPlayWhenReady());
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                player.setPlayWhenReady(true);
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                player.setPlayWhenReady(false);
                break;
            default:
                return false;
        }
        // show();
        return true;
    }*/
/*
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
               // updateMediaDescription(metadata.getDescription());
               // updateDuration(metadata);
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
        //    Log.d(TAG, "ON PLAYBACK STATUS CHANGEDDDDDD IN SMALLPLAYBACKCONTROLVIEW "+state);
            super.onPlaybackStateChanged(state);
            updatePlayPauseButton();
        }

     *//*   @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            switch (event){
                case EVENT_PLAYER_CHANGED:
                    updatePlayPauseButton();
                    break;
            }
        }*//*
    };*/
/*
    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!onConnected!!!!!!!!!!!!!!!!!!!!!!!!");
                    try {
                        connectToSession(mMediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(TAG, e+ "could not connect media controller");
                    }
                }
            };

    public void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(
                mContext, token);
        *//*if (mediaController.getMetadata() == null) {
            activityCallbacks.finishActivity();
            return;
        }
*//*        activityCallbacks.setSupportMediaControllerForActivity(mediaController);
        mediaController.registerCallback(mCallback);

        MediaMetadataCompat metadata = mediaController.getMetadata();

        if (metadata != null) {
         //   updateMediaDescription(metadata.getDescription());
           // updateDuration(metadata);
        }
    }*/
/*
    public void disconnectSession() {
        if(mMediaBrowser.isConnected()) {
            Log.d(TAG, "@@@@@@ DISCONNECT FROM THE MEDIA BROWSERRRRRRR @@@@@@@@@@@");
            mMediaBrowser.disconnect();
        }
    }*/

/*
    final class ComponentListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            PlaybackStateCompat state = ((FragmentActivity)mContext).getSupportMediaController().getPlaybackState();
            boolean playing = state.getState() == PlaybackStateCompat.STATE_PLAYING;
            Log.d(TAG, "ON CLICK ON PLAY BUTTON isPlaying "+playing+state.getState());
           // if (state != null)
            {
                MediaControllerCompat.TransportControls controls =
                        ((FragmentActivity)mContext).getSupportMediaController().getTransportControls();
                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING: // fall through
                    case PlaybackStateCompat.STATE_BUFFERING:
                        controls.pause();
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                    case PlaybackStateCompat.STATE_STOPPED:
                    case PlaybackStateCompat.STATE_NONE:
                        controls.play();
                        break;
                    default:
                        Log.d(TAG, "onClick with state "+ state.getState());
                }
            }

        }
    }*/

}
