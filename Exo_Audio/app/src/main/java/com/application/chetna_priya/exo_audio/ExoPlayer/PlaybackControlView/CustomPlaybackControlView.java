package com.application.chetna_priya.exo_audio.ExoPlayer.PlaybackControlView;

import android.content.ComponentName;
import android.content.Context;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.application.chetna_priya.exo_audio.ExoPlayer.PlayerService.PodcastService;
import com.application.chetna_priya.exo_audio.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.util.Util;
import java.util.Formatter;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A view to control video playback of an {@link ExoPlayer}.
 */
public class CustomPlaybackControlView extends AbstractPlaybackControlView{

    private static final int PROGRESS_BAR_MAX = 1000;
    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;
    private static final String TAG = CustomPlaybackControlView.class.getSimpleName();
    private final MediaBrowserCompat mMediaBrowser;

    @BindView(R.id.tv_time) TextView time;
    @BindView(R.id.tv_time_current) TextView timeCurrent;
    @BindView(R.id.tv_speed) TextView speed;
    @BindView(R.id.btn_play) ImageButton playButton;
    @BindView(R.id.btn_prev) View previousButton;
    @BindView(R.id.btn_next) View nextButton;
    @BindView(R.id.btn_rew) View rewindButton;
    @BindView(R.id.btn_ffwd) View fastForwardButton;
    @BindView(R.id.seek_mediacontroller_progress) SeekBar progressBar;

    private PlaybackStateCompat mLastPlaybackState;

    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private final Timeline.Window currentWindow;
    private final boolean speedViewVisible;
    private final float[] SPEED_ARR = {0.50f,0.60f,0.70f,0.80f,0.90f,1.00f,1.10f,1.20f,1.30f,1.40f,1.50f,
                                        1.60f,1.70f,1.80f,1.90f,2.00f};
    private final int STANDARD_INDEX = 5;//index of 1.00f
    private int CURRENT_INDEX = STANDARD_INDEX;


    private boolean dragging;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };
    private Context mContext;

    public CustomPlaybackControlView(Context context) {
        this(context, null);
    }

    public CustomPlaybackControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomPlaybackControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        currentWindow = new Timeline.Window();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        ComponentListener componentListener = new ComponentListener();

        View view  = LayoutInflater.from(context).inflate(R.layout.custom_exo_playback_control_view, this);
        ButterKnife.bind(this, view);
        if(Util.SDK_INT < 23){
            speedViewVisible = false;
            speed.setVisibility(View.GONE);
        }else {
            CURRENT_INDEX = STANDARD_INDEX;
            updateSpeedText();
            speed.setOnClickListener(componentListener);
            speedViewVisible = true;
        }

        playButton.setOnClickListener(componentListener);
        previousButton.setOnClickListener(componentListener);
        nextButton.setOnClickListener(componentListener);
        rewindButton.setOnClickListener(componentListener);
        fastForwardButton.setOnClickListener(componentListener);
        progressBar.setOnSeekBarChangeListener(componentListener);
        progressBar.setMax(PROGRESS_BAR_MAX);

        mMediaBrowser = new MediaBrowserCompat(mContext,
                new ComponentName(mContext, PodcastService.class), mConnectionCallback, null);
        activityCallbacks.setMediaBrowser(mMediaBrowser);
    }


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
            super.onPlaybackStateChanged(state);
            mLastPlaybackState = state;
            updateAll();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            switch (event){
                case EVENT_POSITION_DISCONTINUITY:
                    updateNavigation();
                    updateProgress();
                    break;
                case EVENT_SPEED_CHANGE:
                    updateNavigation();
                    updateProgress();
                    break;
                case EVENT_TIME_LINE_CHANGED:
                    updateNavigation();
                    updateProgress();
                    break;
                case EVENT_PLAYER_CHANGED:
                    updatePlayPauseButton();
                    updateProgress();
                    break;
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected");
                    try {
                        connectToSession(mMediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        Log.e(TAG, e+ "could not connect media controller");
                    }
                }
            };

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(
                mContext, token);
        if (mediaController.getMetadata() == null) {
            activityCallbacks.finishActivity();
            return;
        }
        activityCallbacks.setSupportMediaControllerForActivity(mediaController);
        mediaController.registerCallback(mCallback);
        mLastPlaybackState = mediaController.getPlaybackState();
        updateAll();
        MediaMetadataCompat metadata = mediaController.getMetadata();
        if (metadata != null) {
            //   updateMediaDescription(metadata.getDescription());
            // updateDuration(metadata);
        }/*
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }*/
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateProgress();
    }

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

    }


    private void updateNavigation() {

        MediaControllerCompat mediaControllerCompat= ((FragmentActivity)mContext).getSupportMediaController();
        MediaControllerCompat.TransportControls controls =mediaControllerCompat.getTransportControls();
        PlaybackStateCompat state = mediaControllerCompat.getPlaybackState();
        MediaMetadataCompat metadata = mediaControllerCompat.getMetadata();
        long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        long currentPosition = state.getPosition();
        //TODO come up with a robust implementation below
      //  Timeline currentTimeline = player != null ? player.getCurrentTimeline() : null;
        boolean haveTimeline = currentPosition < duration/*currentTimeline != null*/;
        boolean isSeekable = false;
        boolean enablePrevious = false;
        boolean enableNext = false;
        if (haveTimeline) {
          //  int currentWindowIndex = player.getCurrentWindowIndex();
           // currentTimeline.getWindow(currentWindowIndex, currentWindow);
            isSeekable = currentPosition < duration /*&&
                    android.os.SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime()<= 30000*/;
            /*currentWindow.isSeekable*/;
         //   enablePrevious = currentWindowIndex > 0 || isSeekable || !currentWindow.isDynamic;
          //  enableNext = (currentWindowIndex < currentTimeline.getWindowCount() - 1)
            //        || currentWindow.isDynamic;
        }
        Log.d(TAG, "TIMELINEEEEEEE "+haveTimeline+" isSeekable "+isSeekable);
        Log.d(TAG, "CURRENT POSITIONNNN "+currentPosition+" currentPosition < duration - 30000 "+duration);
        setButtonEnabled(enablePrevious , previousButton);
        setButtonEnabled(enableNext, nextButton);
        setButtonEnabled(isSeekable, fastForwardButton);
        setButtonEnabled(isSeekable, rewindButton);
        if(speedViewVisible)
            setButtonEnabled(isSeekable,speed);
    //    Log.d(TAG, "Previous Button "+enablePrevious);
     //   Log.d(TAG, "Next Button "+enablePrevious);
      //  Log.d(TAG, "Forward Button "+enablePrevious);
       // Log.d(TAG, "Rewind Button "+enablePrevious);
        progressBar.setEnabled(isSeekable);
    }

    private void updateProgress() {

        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        MediaControllerCompat mediaControllerCompat= ((FragmentActivity)mContext).getSupportMediaController();

        if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }

        time.setText(stringForTime(mediaControllerCompat.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
        Log.d(TAG, "UPDATE TIME TEXT; dragging "+dragging+" POSITION : "+currentPosition);
        if (!dragging) {
            timeCurrent.setText(stringForTime(currentPosition));
        }
        if (!dragging) {
            progressBar.setProgress(progressBarValue(currentPosition));
        }
      //  progressBar.setSecondaryProgress(progressBarValue(currentPosition+mLastPlaybackState.getBufferedPosition()));
        // Remove scheduled updates.
        removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
     //   int playbackState = player == null ? ExoPlayer.STATE_IDLE : player.getPlaybackState();
        if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_NONE
                && mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED
                && mLastPlaybackState.getState() != PlaybackStateCompat.STATE_STOPPED &&
                mLastPlaybackState.getState() != PlaybackStateCompat.STATE_ERROR) {
            long delayMs;
            if (mLastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING
                /* && state.getState() == ExoPlayer.STATE_READY*/) {
                delayMs = 1000 - (mLastPlaybackState.getPosition() % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            postDelayed(updateProgressAction, delayMs);
        }
    }

    private void setButtonEnabled(boolean enabled, View view) {
        view.setEnabled(enabled);
        if (Util.SDK_INT >= 11) {
            view.setAlpha(enabled ? 1f : 0.3f);
            view.setVisibility(VISIBLE);
        } else {
            view.setVisibility(enabled ? VISIBLE : INVISIBLE);
        }
    }

    private String stringForTime(long timeMs) {
        if (timeMs == C.TIME_UNSET) {
            timeMs = 0;
        }
        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    private int progressBarValue(long position) {

        MediaControllerCompat mediaControllerCompat= ((FragmentActivity)mContext).getSupportMediaController();
        PlaybackStateCompat state = mediaControllerCompat.getPlaybackState();

        long duration = state.getState() == PlaybackStateCompat.STATE_ERROR  ? C.TIME_UNSET :
                mediaControllerCompat.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        return duration == C.TIME_UNSET || duration == 0 ? 0
                : (int) ((position * PROGRESS_BAR_MAX) / duration);
    }

    private long positionValue(int progress) {
        MediaControllerCompat mediaControllerCompat= ((FragmentActivity)mContext).getSupportMediaController();
        PlaybackStateCompat state = mediaControllerCompat.getPlaybackState();

        long duration = state.getState() == PlaybackStateCompat.STATE_ERROR  ? C.TIME_UNSET :
                mediaControllerCompat.getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        return duration == C.TIME_UNSET ? 0 : ((duration * progress) / PROGRESS_BAR_MAX);
    }

    private void updateSpeedText() {
        speed.setText(SPEED_ARR[CURRENT_INDEX]+"x");
    }

   /* @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (player == null || event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                fastForward();
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                rewind();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if(hasAudioFocus(player))
                    player.setPlayWhenReady(!player.getPlayWhenReady());
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if(hasAudioFocus(player))
                    player.setPlayWhenReady(true);
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if(hasAudioFocus(player))
                    player.setPlayWhenReady(false);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                next();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                previous();
                break;
            default:
                return false;
        }
       // show();
        return true;
    }
*/

    final class ComponentListener implements  SeekBar.OnSeekBarChangeListener, OnClickListener{

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            dragging = true;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                timeCurrent.setText(stringForTime(positionValue(progress)));
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            dragging = false;

            MediaControllerCompat.TransportControls controls =
                    ((FragmentActivity)mContext).getSupportMediaController().getTransportControls();
            controls.seekTo(positionValue(seekBar.getProgress()));
            updateNavigation();
            updateProgress();
           // player.seekTo(positionValue(seekBar.getProgress()));
            //hideDeferred();
        }

        @Override
        public void onClick(View view) {
         //   Timeline currentTimeline = null;
            MediaControllerCompat.TransportControls controls =
                    ((FragmentActivity)mContext).getSupportMediaController().getTransportControls();
           if (nextButton == view) {
                controls.skipToNext();
               // next();
            } else if (previousButton == view) {
                controls.skipToPrevious();

              //  previous();
            } else if (fastForwardButton == view) {
                controls.fastForward();
             //   fastForward();
            } else if (rewindButton == view) {
                controls.rewind();
               // rewind();
            } else if (playButton == view) {
               PlaybackStateCompat state = ((FragmentActivity)mContext).getSupportMediaController().getPlaybackState();
               boolean playing = state.getState() == PlaybackStateCompat.STATE_PLAYING;
               Log.d(TAG, "ON CLICK ON PLAY BUTTON isPlaying "+playing+state.getState());
               // if (state != null)
               {
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

            }else if(speed == view){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(CURRENT_INDEX+1 == SPEED_ARR.length)
                        CURRENT_INDEX = 0;
                    else
                        CURRENT_INDEX++;
                    updateSpeedText();
                    Bundle speedBundle = new Bundle();
                    speedBundle.putFloat(SPEED, SPEED_ARR[CURRENT_INDEX]);
                    controls.sendCustomAction(CUSTOM_ACTION_SPEED_CHANGE, null);
                  }
            }
            //hideDeferred();
        }

    }

}
