package com.raywenderlich.podplay.repository

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
class PodcastRepo(private var feedService: FeedService) {

    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {

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
    // Convert RSS date to Episode and Podcast objects
    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>):
            List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
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
        return Podcast(feedUrl, rssResponse.title, description, imageUrl,
                       rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items))
    }
}