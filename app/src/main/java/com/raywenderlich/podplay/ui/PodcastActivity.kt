package com.raywenderlich.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.*
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import com.raywenderlich.podplay.worker.EpisodeUpdateWorker
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit


class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {

    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()

    val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        addBackStackListener()
        scheduleJobs()
    }

    // Search widget and functionality***********************
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 1
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        // 2
        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView

        searchMenuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }
            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })

        // 3
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        // 4
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (supportFragmentManager.backStackEntryCount > 0) {
            podcastRecyclerView.visibility = View.INVISIBLE
        }

        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }

        return true
    }
    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodcasts(term) {results ->
            hideProgressBar()
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }
    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }

        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let {
                    podcastSummaryView -> onShowDetails(podcastSummaryView)
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }
    //*******************************************************

    // Link ViewModels and such******************************
    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        val rssService = FeedService.instance
        //podcastViewModel.podcastRepo = PodcastRepo(rssService)
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
    }
    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(podcastRecyclerView.context,
            layoutManager.orientation)
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null,this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }
    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        // 1
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        // 2
        showProgressBar()
        // 3
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            // 4
            hideProgressBar()
            if (it != null) {
                // 5
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }
    //*******************************************************

    // Progress bar******************************************
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }
    //*******************************************************

    // Create details fragment
    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        // 1
        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?
        // 2
        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    // Show details fragment
    private fun showDetailsFragment() {
        // 1
        val podcastDetailsFragment = createPodcastDetailsFragment()
        // 2
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()
        // 3
        podcastRecyclerView.visibility = View.INVISIBLE
        // 4
        searchMenuItem.isVisible = false
    }

    // Responds to changes in details fragment
    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    // Helper method
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    // SUBSCRIBE
    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }
    // SHOW SUBSCRIPTIONS
    private fun showSubscribedPodcasts() {
        // 1
        val podcasts = podcastViewModel.getPodcasts()?.value
        // 2
        if (podcasts != null) {
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }
    // LATEST SUBS
    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    override fun onUnSubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    // Scheduling
    private fun scheduleJobs() {
        // 1
        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()
        // 2
        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        // 3
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_EPISODE_UPDATE_JOB,
                                ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    override fun onShowEpisodePlayer(episodeViewData: EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    private fun createEpisodePlayerFragment(): EpisodePlayerFragment {
        var episodePlayerFragment =
            supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as EpisodePlayerFragment?

        if (episodePlayerFragment == null) {
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }

        return episodePlayerFragment
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()

        supportFragmentManager.beginTransaction().replace(R.id.podcastDetailsContainer,
            episodePlayerFragment, TAG_PLAYER_FRAGMENT).addToBackStack("PlayerFragment")
            .commit()
        podcastRecyclerView.visibility= View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    // Identifies details fragment
    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }
}