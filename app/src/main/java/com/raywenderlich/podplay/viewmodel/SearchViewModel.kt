package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.service.PodcastResponse
import com.raywenderlich.podplay.util.DateUtils

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var iTunesRepo: ItunesRepo? = null

    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String = "",
        var imageUrl: String = "",
        var feedUrl: String = "")

    // Helper for conversion
    private fun itunesPodcastToPodcastSummaryView(
        itunesPodcast: PodcastResponse.ItunesPodcast) :
            PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl)
    }
    // Perform search
    // 1
    fun searchPodcasts(term: String, callback: (List<PodcastSummaryViewData>) -> Unit) {
        // 2
        iTunesRepo?.serachByTerm(term) { results ->
            if (results ==null) {
                // 3
                callback(emptyList())
            } else {
                // 4
                val searchViews = results.map { podcast ->
                    itunesPodcastToPodcastSummaryView(podcast)
                }
                // 5
                callback(searchViews)
            }
        }
    }
}