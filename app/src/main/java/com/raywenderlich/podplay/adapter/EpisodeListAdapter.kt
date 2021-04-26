package com.raywenderlich.podplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

import kotlinx.android.synthetic.main.episode_item.view.*

interface EpisodeListAdapterListener {
    fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
}

class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
    private var episodeListAdapterListener : EpisodeListAdapterListener) :
    RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

        class ViewHolder(v: View,
                private val episodeListAdapterListener: EpisodeListAdapterListener) :
                RecyclerView.ViewHolder(v) {

            init {
                v.setOnClickListener {
                    episodeViewData?.let {
                        episodeListAdapterListener.onSelectedEpisode(it)
                    }
                }
            }

            var episodeViewData: PodcastViewModel.EpisodeViewData? = null
            var titleTextView: TextView = v.titleView
            var descTextView: TextView = v.descView
            var durationTextView: TextView = v.durationView
            var releaseDateTextView: TextView = v.releaseDateView
        }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): EpisodeListAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.episode_item, parent, false),
                episodeListAdapterListener)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]

        holder.episodeViewData = episodeView
        holder.titleTextView.text = episodeView.title
        holder.descTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releaseDate?.let {
            DateUtils.dateToShortDate(it)
        }
    }
    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }
}
