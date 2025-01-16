package com.example.dovzhenkotv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.graphics.drawable.Drawable
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.Serializable

import java.util.Collections

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {

    private var mSelectedMovie: Movie? = null

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter
    private lateinit var mActionAdapter: ArrayObjectAdapter
    private var mCookies: Map<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)
        mCookies = HashMap<String, String>()

        // Get selected movie and cookies
        mSelectedMovie = activity!!.intent.getSerializableExtra(DetailsActivity.MOVIE) as Movie
        val tempCookie = activity!!.intent.getSerializableExtra(DetailsActivity.COOKIE)
        if (tempCookie != null) {
            mCookies = activity!!.intent.getSerializableExtra(DetailsActivity.COOKIE) as Map<String, String>
        }

        // Setup selected movie
        if (mSelectedMovie != null) {
            mPresenterSelector = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(mPresenterSelector)
            setupDetailsOverviewRow()
            setupDetailsOverviewRowPresenter()
            adapter = mAdapter
            initializeBackground(mSelectedMovie)
            loadMovieData()
        } else {
            val intent = Intent(context!!, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun loadMovieData() {
        if (mSelectedMovie?.infoUrl != null) {
            // Get information about movie
            Thread({
                val url = mSelectedMovie!!.infoUrl
                val movie = Jsoup.connect(url!!).cookies(mCookies).method(Connection.Method.GET).execute()
                mCookies = movie.cookies()
                val movieData = movie.parse()
                val buttons =
                    movieData.select(".elementor-widget-button[data-widget_type=\"button.default\"] > div > div > .elementor-button.elementor-button-link.elementor-size-sm")
                val hasMovie = buttons.size == 3
                val hasTeaser = buttons.size > 1

                activity?.runOnUiThread {
                    mSelectedMovie?.description =
                        movieData.select(".elementor-widget.elementor-widget-text-editor")[1].text()
                    if (hasTeaser) {
                        mActionAdapter.add(
                            Action(
                                ACTION_WATCH_TRAILER,
                                resources.getString(R.string.watch_trailer_1),
                                resources.getString(R.string.watch_trailer_2)
                            )
                        )
                    }
                    if (hasMovie) {
                        mActionAdapter.add(
                            Action(
                                ACTION_WATCH_MOVIE,
                                resources.getString(R.string.watch_movie_1),
                                resources.getString(R.string.watch_movie_2)
                            )
                        )
                    }
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }
            }).start()
        }
    }
    private fun initializeBackground(movie: Movie?) {
        mDetailsBackground.enableParallax()
        Glide.with(context!!)
            .asBitmap()
            .centerCrop()
            .error(R.drawable.default_background)
            .load(movie?.backgroundImageUrl)
            .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    bitmap: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    mDetailsBackground.coverBitmap = bitmap
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }
            })
    }

    private fun setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie?.toString())
        val row = DetailsOverviewRow(mSelectedMovie)
        row.imageDrawable = ContextCompat.getDrawable(context!!, R.drawable.default_background)
        val width = convertDpToPixel(context!!, DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(context!!, DETAIL_THUMB_HEIGHT)
        Glide.with(context!!)
            .load(mSelectedMovie?.cardImageUrl)
            .centerCrop()
            .error(R.drawable.default_background)
            .into<SimpleTarget<Drawable>>(object : SimpleTarget<Drawable>(width, height) {
                override fun onResourceReady(
                    drawable: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    Log.d(TAG, "details overview card image url ready: " + drawable)
                    row.imageDrawable = drawable
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }
            })

        mActionAdapter = ArrayObjectAdapter()

        row.actionsAdapter = mActionAdapter

        mAdapter.add(row)
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(context!!, R.color.selected_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
            activity, DetailsActivity.SHARED_ELEMENT_NAME
        )
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        // Handling actions
        detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->
            if (action.id == ACTION_WATCH_TRAILER) {
                val intent = Intent(context!!, WebVideoPlayback::class.java)
                intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
                intent.putExtra(DetailsActivity.COOKIE, mCookies as Serializable)
                intent.putExtra(DetailsActivity.VIDEO_TYPE, "trailer")
                startActivity(intent)
            } else {
                if (action.id == ACTION_WATCH_MOVIE) {
                    val intent = Intent(context!!, WebVideoPlayback::class.java)
                    intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
                    intent.putExtra(DetailsActivity.COOKIE, mCookies as Serializable)
                    intent.putExtra(DetailsActivity.VIDEO_TYPE, "movie")
                    startActivity(intent)
                } else {
                    Toast.makeText(context!!, action.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun convertDpToPixel(context: Context, dp: Int): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    companion object {
        private val TAG = "VideoDetailsFragment"

        private val ACTION_WATCH_TRAILER = 1L
        private val ACTION_WATCH_MOVIE = 2L

        private val DETAIL_THUMB_WIDTH = 274
        private val DETAIL_THUMB_HEIGHT = 274
    }
}