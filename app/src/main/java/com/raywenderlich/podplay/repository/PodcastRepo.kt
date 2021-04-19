package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.model.Podcast

// Retrieve feed from URL
class PodcastRepo {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        callback(Podcast(feedUrl,
            "No Name,",
            "No description",
            "No image"))
    }
}