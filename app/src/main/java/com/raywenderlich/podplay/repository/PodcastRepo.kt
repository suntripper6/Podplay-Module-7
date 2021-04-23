package com.raywenderlich.podplay.repository

import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Retrieve feed from URL
class PodcastRepo(private var feedService: FeedService,
                 private var podcastDao: PodcastDao) {

    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {

        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)

            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {
                feedService.getFeed(feedUrl) { feedResponse ->
                    var podcast: Podcast? = null
                    if (feedResponse != null) {
                        podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                    }
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            }
        }
    }
    // Convert RSS date to Episode and Podcast objects
    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>):
            List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }
    private fun rssResponseToPodcast(feedUrl: String, imageUrl: String,
                                    rssResponse: RssFeedResponse): Podcast? {
        // 1
        val items = rssResponse.episodes ?: return null
        // 2
        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description
        // 3
        return Podcast(null, feedUrl, rssResponse.title, description, imageUrl,
                        rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items))
    }

    // Inserts Podcast and Episodes into the DB
    fun save(podcast: Podcast) {
        GlobalScope.launch {
            // 1
            val podcastId = podcastDao.insertPodcast(podcast)
            // 2
            for (episode in podcast.episodes) {
                // 3
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }
    // Deletes Podcast
    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    // Retrieves from DB
    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }
}