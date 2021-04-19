package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import java.util.*

// For fragment
class PodcastViewModel(application: Application) : AndroidViewModel(application) {
    var podcastRepo: PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null

    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feeTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = null
    )
    // Conversion to PodcastViewData view objects
    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            EpisodeViewData(it.guid, it.title, it.description,
                            it.mediaUrl, it.releaseDate, it.duration)
        }
    }
    // Conversion to PodcastViewData object
    private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
        return PodcastViewData(false, podcast.feedTitle, podcast.feedUrl,
                                podcast.feedDesc, podcast.imageUrl,
                                episodesToEpisodesView(podcast.episodes))
    }
    // Retrieve podast from Repo
    // 1
    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData,
                    callback: (PodcastViewData?) -> Unit) {
        // 2
        val repo = podcastRepo?: return
        val feedUrl = podcastSummaryViewData.feedUrl?: return
        // 3
        repo.getPodcast(feedUrl) {
            // 4
            it?.let {
                // 5
                it.feedTitle = podcastSummaryViewData.name ?: ""
                // 6
                it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                // 7
                activePodcastViewData = podcastToPodcastView(it)
                // 8
                callback(activePodcastViewData)
            }
        }
    }
}