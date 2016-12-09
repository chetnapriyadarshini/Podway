package com.application.chetna_priya.exo_audio.Ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.application.chetna_priya.exo_audio.R;
import com.application.chetna_priya.exo_audio.Utils.PreferenceHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenreActivity extends AppCompatActivity implements GenreAdapter.Listener {

    public static final String IS_FIRST_TIME = "is_first_time";
    @BindView(R.id.my_recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.btn_genre)
    Button genreButton;

    final int MIN_SELECTED_GENRES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre);
        ButterKnife.bind(this);
      //  mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(false);

        // use a linear layout manager
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, getResources().getInteger(R.integer.genre_grid_span_count));
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        final GenreAdapter mAdapter = new GenreAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFocusable(false);

        if(getIntent().getBooleanExtra(IS_FIRST_TIME, true)) {
            genreButton.setEnabled(false);
            onGenreSaved(0);
        }

        genreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreferenceHelper.saveGenrePreferences(getApplicationContext(), mAdapter.getSavedGenres());
                setResult(RESULT_OK);
                finish();
                /*
                Intent intent = new Intent(GenreActivity.this,tabbed.class);
                startActivity(intent);*/
            }
        });
    }

    @Override
    public void onGenreSaved(int selectedGenres) {
        if(selectedGenres < MIN_SELECTED_GENRES) {
          //  genreButton.setBackgroundColor(R.color.white);
          //  genreButton.setTextColor(R.color.white);
            switch (selectedGenres) {
                case 0:
                    genreButton.setText(getResources().getString(R.string.select_three_genre));
                    break;
                case 1:
                    genreButton.setText(getResources().getString(R.string.nice_start));
                    break;
                case 2:
                    genreButton.setText(getResources().getString(R.string.almost_there));
                    break;
            }
        }else
        {
            genreButton.setEnabled(true);
            genreButton.setText(R.string.label_next);
        //    genreButton.setBackgroundColor(R.color.colorAccent);
         //   genreButton.setTextColor(R.color.white);
        }
        genreButton.setContentDescription(genreButton.getText());
    }
}