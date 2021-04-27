package com.raywenderlich.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.EpisodeListAdapter
import com.raywenderlich.podplay.adapter.EpisodeListAdapterListener
import com.raywenderlich.podplay.service.PodplayMediaService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.*
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import java.lang.RuntimeException

// Displays podcast details
class PodcastDetailsFragment : Fragment(), EpisodeListAdapterListener {

    private var menuItem: MenuItem? = null
    private var listener: OnPodcastDetailsListener? = null
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private val podcastViewModel: PodcastViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    // 2
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
        menuItem = menu.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    // User interface controls
    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feeTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl).into(feedImageView)
        }
    }

    private fun setupControls() {
        // 1
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        // 2
        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            episodeRecyclerView.context,
            layoutManager.orientation
        )
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)
        // 3
        episodeListAdapter =
            EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes, this)
        episodeRecyclerView.adapter = episodeListAdapter
    }

    // SUBSCRIPTIONS
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnPodcastDetailsListener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_action -> {
                if (item.title == getString(R.string.unsubscribe)) {
                    listener?.onUnSubscribe()
                } else {
                    listener?.onSubscribe()
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuItem() {
        // 1
        val viewData = podcastViewModel.activePodcastViewData ?: return
        // 2
        menuItem?.title = if (viewData.subscribed)
            getString(R.string.unsubscribe) else
            getString(R.string.subscribe)
    }

    // Instance of a fragment
    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    override fun onStart() {
        super.onStart()

    }
    override fun onStop() {
        super.onStop()
    }

    override fun onSelectedEpisode(episodeViewData: EpisodeViewData) {
       listener?.onShowEpisodePlayer(episodeViewData)
    }

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnSubscribe()
        fun onShowEpisodePlayer(episodeViewData: EpisodeViewData)
    }
}