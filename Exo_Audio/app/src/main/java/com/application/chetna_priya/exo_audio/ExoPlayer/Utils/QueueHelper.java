package com.application.chetna_priya.exo_audio.ExoPlayer.Utils;

import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.application.chetna_priya.exo_audio.data.PodcastProvider;

import java.util.ArrayList;
import java.util.List;

import static com.application.chetna_priya.exo_audio.ExoPlayer.Utils.MediaIDHelper.MEDIA_ID_PODCASTS_BY_SEARCH;


/**
 * Utility class to help on queue related tasks.
 */
public class QueueHelper {

    private static final String TAG = QueueHelper.class.getSimpleName();

    private static final int RANDOM_QUEUE_SIZE = 10;

    public static List<MediaSessionCompat.QueueItem> getPlayingQueue(String mediaId,
                                                                     PodcastProvider podcastProvider) {

        // extract the browsing hierarchy from the media ID:
        String[] hierarchy = MediaIDHelper.getHierarchy(mediaId);

        if (hierarchy.length != 2) {
            Log.e(TAG, "Could not build a playing queue for this mediaId: "+ mediaId);
            return null;
        }

        String categoryType = hierarchy[0];
        String categoryValue = hierarchy[1];
        Log.d(TAG, "Creating playing queue for "+ categoryType+ ",  "+ categoryValue);

        Iterable<MediaMetadataCompat> tracks = null;
        // This sample only supports genre and by_search category types.
        if (categoryType.equals(MediaIDHelper.MEDIA_ID_PODCASTS_BY_GENRE)) {
            tracks = podcastProvider.getPodcastsByGenre(categoryValue);
        } else if (categoryType.equals(MediaIDHelper.MEDIA_ID_PODCASTS_BY_SEARCH)) {
            tracks = podcastProvider.searchPodcastByPodcastTitle(categoryValue);
        }

        if (tracks == null) {
            Log.e(TAG, "Unrecognized category type: "+ categoryType+ " for media "+ mediaId);
            return null;
        }

        return convertToQueue(tracks, hierarchy[0], hierarchy[1]);
    }

    public static List<MediaSessionCompat.QueueItem> getPlayingQueueFromSearch(String query,
                                                                               Bundle queryParams, PodcastProvider podcastProvider) {

        Log.d(TAG, "Creating playing queue for musics from search: "+ query+
                " params="+ queryParams);
        Iterable<MediaMetadataCompat> result = null;

        // If there was no results using media focus parameter, we do an unstructured query.
        // This is useful when the user is searching for something that looks like an artist
        // to Google, for example, but is not. For example, a user searching for Madonna on
        // a PodCast application wouldn't get results if we only looked at the
        // Artist (podcast author). Then, we can instead do an unstructured search.
       // if (params.isUnstructured || result == null || !result.iterator().hasNext())
        {
            // To keep it simple for this example, we do unstructured searches on the
            // song title only. A real world application could search on other fields as well.
            result = podcastProvider.searchPodcastByPodcastTitle(query);
        }

        return convertToQueue(result, MEDIA_ID_PODCASTS_BY_SEARCH, query);
    }


    public static int getPodcastIndexOnQueue(Iterable<MediaSessionCompat.QueueItem> queue,
                                           String mediaId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int getPodcastIndexOnQueue(Iterable<MediaSessionCompat.QueueItem> queue,
                                           long queueId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static List<MediaSessionCompat.QueueItem> convertToQueue(
            Iterable<MediaMetadataCompat> tracks, String... categories) {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadataCompat track : tracks) {

            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                    track.getDescription().getMediaId(), categories);

            MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                    .build();

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(
                    trackCopy.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }

/*    public static List<MediaSessionCompat.QueueItem> getRandomQueue(PodcastProvider podcastProvider) {
        List<MediaMetadataCompat> result = new ArrayList<>(RANDOM_QUEUE_SIZE);
        Iterable<MediaMetadataCompat> shuffled = podcastProvider.getShuffledPodcast();
        for (MediaMetadataCompat metadata: shuffled) {
            if (result.size() == RANDOM_QUEUE_SIZE) {
                break;
            }
            result.add(metadata);
        }
        Log.d(TAG, "getRandomQueue: result.size="+ result.size());

        return convertToQueue(result, MEDIA_ID_PODCASTS_BY_SEARCH, "random");
    }*/

    public static boolean isIndexPlayable(int index, List<MediaSessionCompat.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }
}
