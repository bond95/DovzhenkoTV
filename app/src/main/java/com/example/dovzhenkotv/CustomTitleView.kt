package com.example.dovzhenkotv


import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnKeyListener
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.leanback.widget.SearchOrbView
import androidx.leanback.widget.TitleViewAdapter

/**
 * Custom title view to be used in [androidx.leanback.app.BrowseFragment].
 */
class CustomTitleView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    RelativeLayout(context, attrs, defStyle), TitleViewAdapter.Provider {

    private val mFocusedZoom = 1.2
    private val mScaleDurationMs = 100

    val root: View = LayoutInflater.from(context).inflate(R.layout.tv_titleview, this)
    private val mTitleView: TextView = root.findViewById(R.id.title_text)
    val logoView: ImageView = root.findViewById(R.id.title_photo_contact)
    private val mSearchOrbView: View = root.findViewById<SearchOrbView>(R.id.title_orb).apply {
        setOnKeyListener(OnKeyListener { v, keyCode, event ->
            false
        })
    }
    val loginButton: ImageButton = root.findViewById<ImageButton?>(R.id.title_login).apply {
        this.setOnFocusChangeListener(OnFocusChangeListener {v, hasFocus ->
            val zoom = if (hasFocus) mFocusedZoom.toFloat() else 1f
            this.animate().scaleX(zoom).scaleY(zoom).setDuration(mScaleDurationMs.toLong())
                .start()
        })
    }


    private val mTitleViewAdapter: TitleViewAdapter = object : TitleViewAdapter() {
        override fun getSearchAffordanceView(): View {
            return mSearchOrbView
        }

        override fun setTitle(titleText: CharSequence?) {
            this@CustomTitleView.setTitle(titleText)
        }
    }

    fun setTitle(title: CharSequence?) {
        if (title == null || title.toString().isEmpty()) {
            Log.e(TAG, "Null title")
            return
        }
        mTitleView.text = title
        mTitleView.visibility = VISIBLE
        logoView.visibility = VISIBLE
        loginButton.visibility = VISIBLE
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }

    companion object {
        private val TAG = CustomTitleView::class.simpleName!!
    }

    init {
        clipChildren = false
        clipToPadding = false
    }
}