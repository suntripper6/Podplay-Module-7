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
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import java.lang.RuntimeException

// Displays podcast details
class PodcastDetailsFragment : Fragment(), EpisodeListAdapterListener {
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var menuItem: MenuItem? = null
    private var listener: OnPodcastDetailsListener? = null
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private val podcastViewModel: PodcastViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1
        setHasOptionsMenu(true)
        initMediaBrowser()
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

    // For media browser stuff - callback
    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
        }
    }
    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            // 2
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            // Disable transport controls
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            // Fata error handling
        }
    }
    private fun registerMediaController(token: MediaSessionCompat.Token) {
        // 1
        val fragmentActivity = activity as FragmentActivity
        // 2
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        // 3
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        // 4
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }
    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity,
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            MediaBrowserCallBacks(), null)
    }
    override fun onStart() {
        super.onStart()
        if (mediaBrowser.isConnected) {
            val fragmentActivity = activity as FragmentActivity
            if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                registerMediaController(mediaBrowser.sessionToken)
            }
        } else {
            mediaBrowser.connect()
        }
    }
    override fun onStop() {
        super.onStop()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }
    }
    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        //controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), null)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.feeTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.imageUrl)

        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        // 1
        val fragmentActivity = activity as FragmentActivity
        // 2
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        // 3
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                // 4
                controller.transportControls.pause()
            } else {
                // 5
                startPlaying(episodeViewData)
            }
        } else {
            // 6
            startPlaying(episodeViewData)
        }
    }

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnSubscribe()
    }
}