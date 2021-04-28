package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.util.DateUtils
import java.util.*

// For fragment
class PodcastViewModel(application: Application) : AndroidViewModel(application) {
    var activeEpisodeViewData: EpisodeViewData? = null
    var livePodcastData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null
    private var activePodcast: Podcast? = null
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
        var duration: String? = null,
        var isVideo: Boolean = false
    )
    // Conversion to PodcastViewData view objects
    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map {
            val isVideo = it.mimeType.startsWith("video")
            EpisodeViewData(it.guid, it.title, it.description,
                            it.mediaUrl, it.releaseDate, it.duration, isVideo)
        }
    }
    // Conversion to PodcastViewData object
    private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
        return PodcastViewData(
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
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
                activePodcast = it
                // 8
                callback(activePodcastViewData)
            }
        }
    }
    // Save active Podcast
    fun saveActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            //it.episodes = it.episodes.drop(1)
            repo.save(it)
        }
    }

    //***BEGIN return view of subscribed Podcasts
    private fun podcastToSummaryView(podcast: Podcast):
            SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            DateUtils.dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl
        )
    }
    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo ?: return null
        // 1
        if (livePodcastData == null) {
            // 2
            val liveData = repo.getAll()
            // 3
            livePodcastData = Transformations.map(liveData) { podcastList ->
                podcastList.map { podcast -> podcastToSummaryView(podcast) }
            }
        }
        // 4
        return livePodcastData
    }
    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let {
            repo.delete(it)
        }
    }

    // Loads podcast from database
    fun setActivePodcast(feedUrl:String,
                         callback: (SearchViewModel.PodcastSummaryViewData?) -> Unit) {
        val repo = podcastRepo ?: return
        repo.getPodcast(feedUrl) {
            if (it == null) {
                callback(null)
            } else {
                activePodcastViewData = podcastToPodcastView(it)
                activePodcast = it
                callback(podcastToSummaryView(it))
            }
        }

    }
}