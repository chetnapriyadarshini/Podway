package com.application.chetna_priya.exo_audio.network;

import android.util.Log;

import com.application.chetna_priya.exo_audio.entity.Podcast;
import com.application.chetna_priya.exo_audio.utils.GenreHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LoadAvailablePodcastChannels {

    private static final String TAG = LoadAvailablePodcastChannels.class.getSimpleName();

    //https://itunes.apple.com/us/rss/topaudiobooks/limit=10/xml
    public ArrayList<Podcast> load(String url, String genre) {

        OkHttpClient client = new OkHttpClient();
//        Log.d(TAG, url);
        /*final String ITUNES_BASE_URL = "https://itunes.apple.com/search?";
        final String TERM_PARAM = "term";
        final String MEDIA_PARAM = "media";
        final String mediaVal = "podcast";
        final String termVal = "comedy";
*/
        try {/*
            *//*if(url == null)*//*{
            Uri.Builder uriBuilder = Uri.parse(ITUNES_BASE_URL).buildUpon();
            uriBuilder.appendQueryParameter(MEDIA_PARAM, mediaVal);
            uriBuilder.appendQueryParameter(TERM_PARAM, termVal);


            Uri builtUri = uriBuilder.build();
                if(url == null)
            url = builtUri.toString();
            }*/
            Log.d(TAG, "URLLLLLLLLLLL " + url);

            Request request = new Request.Builder()
                    .url(new URL(url)).build();

            Response response = client.newCall(request).execute();

            return parseFeedUrlFromJSON(response.body().string(), genre);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<Podcast> parseFeedUrlFromJSON(String responseJSONString, String category) throws JSONException, IOException {
        JSONObject jsonObject = new JSONObject(responseJSONString);
        final String OWM_RESULTS = "results";
        final String OWM_FEED_URL = "feedUrl";
        final String OWM_TRACK_ID = "trackId";
        final String OWM_ALBUM = "trackName";
        final String OWM_ARTIST = "artistName";
        final String OWM_ARTWORK_URI = "artworkUrl100";
        final String OWM_TOTAL_TRACKS = "trackCount";
        final String OWM_GENRE = "primaryGenreName";

        JSONArray resultArray = jsonObject.getJSONArray(OWM_RESULTS);
        // Log.d(TAG, "RESULT ARRAY LENGTHHHHHHH:::: "+resultArray.length());
        ArrayList<Podcast> podcastList = new ArrayList<>();
        for (int i = 0; i < resultArray.length(); i++) {
            int trackId = -1;
            String album = null;
            String artist = null;
            String artWorkUri = null;
            int totalTracks = -1;
            String result_genre = null;

            if (resultArray.getJSONObject(i).has(OWM_TRACK_ID)) {
                trackId = (int) resultArray.getJSONObject(i).get(OWM_TRACK_ID);
            }
            if (resultArray.getJSONObject(i).has(OWM_ALBUM)) {
                album = (String) resultArray.getJSONObject(i).get(OWM_ALBUM);
            }
            if (resultArray.getJSONObject(i).has(OWM_ARTIST)) {
                artist = (String) resultArray.getJSONObject(i).get(OWM_ARTIST);
            }
            if (resultArray.getJSONObject(i).has(OWM_ARTWORK_URI)) {
                artWorkUri = (String) resultArray.getJSONObject(i).get(OWM_ARTWORK_URI);
            }
            if (resultArray.getJSONObject(i).has(OWM_GENRE)) {
                //First read the primary genre name from the json result
                result_genre = (String) resultArray.getJSONObject(i).get(OWM_GENRE);
                //Get the main genre name, sometimes sub genres are returned
                String mainGenre = GenreHelper.getMainGenreName(result_genre);
                /*Check whether this main genre name equals the genre for which
                  we have fetched the results, if not we set the result_genre to null
                  and do not add this podcast to the category. This is done to rule out
                  irrelevant results and add only the podcasts whose primary genre
                  is the genre we are fetching for or at least belongs to its sub genre categories*/
                if (!(category.equals(mainGenre))) {
                    result_genre = null;
                } else {
                    result_genre = mainGenre;
                }
            }
            if (resultArray.getJSONObject(i).has(OWM_TOTAL_TRACKS)) {
                totalTracks = (int) resultArray.getJSONObject(i).get(OWM_TOTAL_TRACKS);
            }
            if (resultArray.getJSONObject(i).has(OWM_FEED_URL)) {
                final String feedObj = (String) resultArray.getJSONObject(i).get(OWM_FEED_URL);
                //     Log.d(TAG, feedObj);
                //  Log.d(TAG, "INDEXXXXXXXXXXXXX "+(i+1));
                if (result_genre != null) {
                    Podcast podcast = new Podcast(trackId, album, feedObj, artist, result_genre, totalTracks, artWorkUri);
                    podcastList.add(podcast);
                }
            }
        }
        return podcastList;
    }
}
