package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.RssFeedService

// Retrieve feed from URL
class PodcastRepo {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {

        val rssFeedService = RssFeedService()
        rssFeedService.getFeed(feedUrl) {}

            callback(
                Podcast(
                    feedUrl,
                    "No Name,",
                    "No description",
                    "No image"
                )
            )
    }
}