package com.application.chetna_priya.exo_audio.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.application.chetna_priya.exo_audio.R;
import com.application.chetna_priya.exo_audio.data.PodcastContract;
import com.application.chetna_priya.exo_audio.ui.playbackcontrolview.CustomPlaybackControlView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AudioActivity extends AppCompatActivity {

    //  private static final String TAG = AudioActivity.class.getSimpleName();
    public static final String EXTRA_START_FULLSCREEN = "start_full_screen";
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "current_media_info";
    CustomPlaybackControlView customPlaybackControlView;
    MediaBrowserCompat.MediaItem mediaItem;
    MediaMetadataCompat metadataCompat;

    @BindView(R.id.album_img)
    ImageView albumImage;

    @BindView(R.id.tv_episode_title)
    TextView titleView;

    @BindView(R.id.rl_backgr)
    RelativeLayout rlLayout;

    @BindView(R.id.frame_audio)
    FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        customPlaybackControlView = (CustomPlaybackControlView) findViewById(R.id.exo_player_control);
        ButterKnife.bind(this);
        Uri imageUri = null;
        String title = null;
        /*
        Handles case when this activity is started from AllEpisodes Actovoty on selection of an episode
        In this case have access to the mediaId
         */
        if (getIntent().hasExtra(BaseActivity.EXTRA_MEDIA_ITEM))
            mediaItem = getIntent().getParcelableExtra(BaseActivity.EXTRA_MEDIA_ITEM);
        /*
        Handles case when this activity is started from PlaybackControlsFragment on selection of an currently
        playing episode, in this case we have access to the metadata
        */
        else if (getIntent().hasExtra(MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION))
            metadataCompat = getIntent().getParcelableExtra(MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
        /*
        Handling crash on pressing playback controls fragment and directly putting the playback controls fragment
        metadata in parcel. We try to extract the metadata title and image uri and send it across
         */
        else if (getIntent().hasExtra(PlaybackControlsFragment.EXTRA_TITLE)
                && getIntent().hasExtra(PlaybackControlsFragment.EXTRA_IMAGE_URI)) {
            title = getIntent().getStringExtra(PlaybackControlsFragment.EXTRA_TITLE);
            imageUri = Uri.parse(getIntent().getStringExtra(PlaybackControlsFragment.EXTRA_IMAGE_URI));
        }

        if (mediaItem != null) {
            imageUri = Uri.parse(mediaItem.getDescription().getExtras().getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
            title = mediaItem.getDescription().getExtras().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        } else if (metadataCompat != null) {
            imageUri = Uri.parse(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
            title = metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        } else if (getIntent().hasExtra(DownloadFragment.PLAY_FORM_CURSOR_BUNDLE)) {
            Bundle cursorBundle = getIntent().getBundleExtra(DownloadFragment.PLAY_FORM_CURSOR_BUNDLE);
            byte[] imgByte = cursorBundle.getByteArray(DownloadFragment.EXTRA_IMAGE);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length);
            albumImage.setImageBitmap(bitmap);
            titleView.setText(cursorBundle.getString(DownloadFragment.EXTRA_TITLE));
        }
/*
        final ImageView album_img = (ImageView) findViewById(R.id.album_img);
        album_img.setContentDescription(title);
        final TextView titleView = (TextView) findViewById(R.id.tv_episode_title);
        titleView.setContentDescription(title);
        final RelativeLayout rlLayout = (RelativeLayout) findViewById(R.id.rl_backgr);
        final FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_audio);*/

        if (imageUri != null)
            Picasso.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.placeholder)
                    .fit()
                    .into(albumImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            BitmapDrawable bitmapDrawable = (BitmapDrawable) albumImage.getDrawable();
                            Palette.from(bitmapDrawable.getBitmap()).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    int darkcolor = palette.getDarkVibrantColor(Color.BLACK);
                                    int lightcolor = palette.getDarkMutedColor(Color.WHITE);
                                    //The layout containing the description
                                    rlLayout.setBackgroundColor(lightcolor);
                                    //The layout containing the fragment
                                    frameLayout.setBackgroundColor(darkcolor);
                                }
                            });
                        }

                        @Override
                        public void onError() {
                        }
                    });
        if (title != null)
            titleView.setText(title);
    }


    @Override
    protected void onPause() {
        super.onPause();
        customPlaybackControlView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        customPlaybackControlView.onResume();
    }

    //TODO Check launch mode singleTop and onIntent method
        /*@Override
    public void onNewIntent(Intent intent) {
        relaxResources();
        shouldRestorePosition = false;
        setIntent(intent);
    }*/

}
