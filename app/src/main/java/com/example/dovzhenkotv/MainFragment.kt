package com.example.dovzhenkotv

import DataStoreCookies
import android.annotation.SuppressLint
import android.app.Activity
import java.util.Collections
import java.util.Timer
import java.util.TimerTask

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import org.jsoup.Connection
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.math.log

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment(), CoroutineScope {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var mTitleView: CustomTitleView? = null
    private var mCookies: Map<String, String>? = null
    private var datastoreCookies: DataStoreCookies? = null
    private var currentPage = 2
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            // Get cookies after login
            mCookies = it.data?.getStringExtra("cookies")?.split(";")?.map { it.split("=")[0] to it.split("=")[1] }?.toMap()
            launch {
                val cookiesKey = stringPreferencesKey("cookies")
                datastoreCookies!!.putSecurePreference(cookiesKey, mCookies)
            }
            this.loadRows()
        }

    @SuppressLint("UseRequireInsteadOfGet")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)
        // Get cookies secure storage
        datastoreCookies = DataStoreCookies(
            requireContext(),
            SecurityUtil(),
            Gson()
        )

        // Set click handlers for titles document
        mTitleView = view!!.findViewById<CustomTitleView>(R.id.browse_title_group).apply {
            loginButton.setOnClickListener {
                val intent = Intent(context, LoginActivity::class.java)
                getResult.launch(intent)
            }
        }
        launch {
            prepareCookies()
        }

        prepareBackgroundManager()

        setupUIElements()

        loadRows()

        setupEventListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
    }

    private suspend fun prepareCookies() {
        val cookiesKey = stringPreferencesKey("cookies")
        datastoreCookies!!.getSecurePreference(cookiesKey, LinkedTreeMap<String, String>())
            .collect { mCookies = it; Log.d("Cookies", mCookies.toString()) }
    }

    @SuppressLint("UseRequireInsteadOfGet")
    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity!!.window)
        mDefaultBackground = ContextCompat.getDrawable(context!!, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    @SuppressLint("UseRequireInsteadOfGet")
    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(context!!, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(context!!, R.color.search_opaque)
    }

    // Load next movies page
    private fun loadNextPage() {
        Thread({
            val url = "https://online.dovzhenkocentre.org/films/$currentPage/"
            val doc = Jsoup.connect(url).get()
            val movies: Elements = doc.select(".elementor-widget-container > .elementor-grid > article")
            var id = 100 + currentPage * 10
            activity?.runOnUiThread {
                for (movie in movies) {
                    val movieData: Elements = movie.select("a")
                    val img: Elements = movie.select("img")
                    ((rowsAdapter[0] as ListRow).adapter as ArrayObjectAdapter).add(
                        Movie(
                            id.toLong(),
                            title = movieData[1].text(),
                            description = movieData[2].text(),
                            infoUrl = movieData[1].attributes().get("href").toString(),
                            cardImageUrl = img[0].attributes().get("src"),
                            backgroundImageUrl = img[0].attributes().get("src")
                        )
                    )
                    id++
                }
            }
            currentPage++
        }).start()
    }

    // Initial movie's load
    private fun loadRows() {
        Thread({
            // Load initial list of movies
            val url = "https://online.dovzhenkocentre.org/films/"
            val doc = Jsoup.connect(url).get()
            val movies: Elements = doc.select(".elementor-widget-container > .elementor-grid > article")
            var id = 100
            activity?.runOnUiThread {
                rowsAdapter.clear()
                val cardPresenter = CardPresenter()
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                val header = HeaderItem(ALL_ROW_ID.toLong(), resources.getString(R.string.all_movies))
                for (movie in movies) {
                    val movieData: Elements = movie.select("a")
                    val img: Elements = movie.select("img")

                    listRowAdapter.add(
                        Movie(
                            id.toLong(),
                            title = movieData[1].text(),
                            description = movieData[2].text(),
                            infoUrl = movieData[1].attributes().get("href").toString(),
                            cardImageUrl = img[0].attributes().get("src"),
                            backgroundImageUrl = img[0].attributes().get("src")
                        )
                    )
                    id++
                }
                rowsAdapter.add(ListRow(header, listRowAdapter))
            }
            // Load bought movies if user logged in
            if (mCookies != null) {
                val urlBought = "https://online.dovzhenkocentre.org/my-account/?ihc_ap_menu=subscription"
                val docBought = Jsoup.connect(urlBought).cookies(mCookies).method(Connection.Method.GET).execute()

                val boughtMovies: Elements = docBought.parse().select(".ihc-account-subscr-list > tbody > tr")

                var id = 100
                var parsedMovies = mutableListOf<Movie>()
                for (movie in boughtMovies) {
                    val movieData: Elements = movie.select("td")
                    var titleElement: Elements = movieData[0].select("a")

                    val boughtMovie = Jsoup.connect(titleElement[0].attributes().get("href").toString()).cookies(mCookies).method(Connection.Method.GET).execute()

                    val page = boughtMovie.parse()
                    val image = page.select(".e-con-full.e-flex.e-con.e-parent")

                    parsedMovies.add(
                        Movie(
                            id.toLong(),
                            title = titleElement[0].text(),
                            description = page.select(".elementor-widget.elementor-widget-text-editor")[1].text(),
                            infoUrl = titleElement[0].attributes().get("href").toString(),
                            cardImageUrl = image[0].attributes().get("data-dce-background-image-url").toString(),
                            backgroundImageUrl = image[0].attributes().get("data-dce-background-image-url").toString()
                        )
                    )
                    id++
                }
                activity?.runOnUiThread {
                    val cardPresenter = CardPresenter()
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                    val header = HeaderItem(BOUGHT_ROW_ID.toLong(), resources.getString(R.string.bought_movies))
                    listRowAdapter.addAll(0, parsedMovies)
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }
            }
        }).start()

        adapter = rowsAdapter
    }

    @SuppressLint("UseRequireInsteadOfGet")
    private fun setupEventListeners() {
        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {

            if (item is Movie) {
                val intent = Intent(context!!, DetailsActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)
                if (mCookies != null) {
                    intent.putExtra(DetailsActivity.COOKIE, mCookies as Serializable)
                }

                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity!!,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    DetailsActivity.SHARED_ELEMENT_NAME
                )
                    .toBundle()
                startActivity(intent, bundle)
            } else if (item is String) {
                if (item.contains(getString(R.string.error_fragment))) {
                    val intent = Intent(context!!, BrowseErrorActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(context!!, item, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // On movie selection change handler
    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            val indexOfRow = rowsAdapter.indexOf(row)
            val indexOfItem = ((row as ListRow).adapter as ArrayObjectAdapter).indexOf(item)
            if (item is Movie) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }
            if (indexOfRow == 0 && indexOfItem > 0 && indexOfItem == ((row as ListRow).adapter as ArrayObjectAdapter).size() -1 ) {
                loadNextPage()
            }
        }
    }

    @SuppressLint("UseRequireInsteadOfGet")
    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(context!!)
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        mBackgroundManager.drawable = drawable
                    }
                })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val ALL_ROW_ID = 0
        private val BOUGHT_ROW_ID = 1
    }
}