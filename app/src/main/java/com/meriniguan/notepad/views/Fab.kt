package com.meriniguan.notepad.views

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.recyclerview.widget.RecyclerView
import com.getbase.floatingactionbutton.AddFloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.meriniguan.notepad.R
import com.meriniguan.notepad.utils.OnFabItemClickedListener

class Fab(fabView: View?, recyclerView: RecyclerView, expandOnLongClick: Boolean) {
    private val floatingActionsMenu: FloatingActionsMenu?
    private val recyclerView: RecyclerView
    private val expandOnLongClick: Boolean
    private var fabAllowed = false
    private var fabHidden = false
    var isExpanded = false
        private set
    private var overlay: View? = null
    private var onFabItemClickedListener: OnFabItemClickedListener? = null
    private fun init() {
        fabHidden = false
        isExpanded = false
        val fabAddButton = floatingActionsMenu!!
            .findViewById<AddFloatingActionButton>(R.id.fab_expand_menu_button)
        fabAddButton.setOnClickListener { v: View ->
            if (!isExpanded && expandOnLongClick) {
                performAction(v)
            } else {
                performToggle()
            }
        }
        fabAddButton.setOnLongClickListener { v: View ->
            if (!expandOnLongClick) {
                performAction(v)
            } else {
                performToggle()
            }
            true
        }
        recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        hideFab()
                    } else if (dy < 0) {
                        floatingActionsMenu!!.collapse()
                        showFab()
                    } else {
                        //LogDelegate.d("No Vertical Scrolled")
                    }
                }
            })
        floatingActionsMenu!!.findViewById<View>(R.id.fab_checklist)
            .setOnClickListener(onClickListener)
        floatingActionsMenu.findViewById<View>(R.id.fab_camera).setOnClickListener(onClickListener)
        if (!expandOnLongClick) {
            val noteBtn = floatingActionsMenu.findViewById<View>(R.id.fab_note)
            noteBtn.visibility = View.VISIBLE
            noteBtn.setOnClickListener(onClickListener)
        }
    }

    private val onClickListener =
        View.OnClickListener { v -> onFabItemClickedListener?.onFabItemClick(v.id) }

    init {
        floatingActionsMenu = fabView as FloatingActionsMenu?
        this.recyclerView = recyclerView
        this.expandOnLongClick = expandOnLongClick
        init()
    }

    fun performToggle() {
        isExpanded = !isExpanded
        floatingActionsMenu!!.toggle()
    }

    private fun performAction(v: View) {
        if (isExpanded) {
            floatingActionsMenu!!.toggle()
            isExpanded = false
        } else {
            onFabItemClickedListener?.onFabItemClick(v.id)
        }
    }

    fun showFab() {
        if (floatingActionsMenu != null && fabHidden) {
            animateFab(0, View.VISIBLE, View.VISIBLE)
            fabHidden = false
        }
    }

    fun hideFab() {
        if (floatingActionsMenu != null && !fabHidden) {
            floatingActionsMenu.collapse()
            animateFab(
                floatingActionsMenu.height + getMarginBottom(floatingActionsMenu),
                View.VISIBLE, View.INVISIBLE
            )
            fabHidden = true
            isExpanded = false
        }
    }

    private fun animateFab(translationY: Int, visibilityBefore: Int, visibilityAfter: Int) {
        ViewCompat.animate(floatingActionsMenu!!)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(FAB_ANIMATION_TIME)
            .translationY(translationY.toFloat())
            .setListener(object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {
                    floatingActionsMenu.visibility = visibilityBefore
                }

                override fun onAnimationEnd(view: View) {
                    floatingActionsMenu.visibility = visibilityAfter
                }

                override fun onAnimationCancel(view: View) {
                    // Nothing to do
                }
            })
    }

    fun setAllowed(allowed: Boolean) {
        fabAllowed = allowed
    }

    private fun getMarginBottom(view: View): Int {
        var marginBottom = 0
        val layoutParams = view.layoutParams
        if (layoutParams is MarginLayoutParams) {
            marginBottom = layoutParams.bottomMargin
        }
        return marginBottom
    }

    fun setOnFabItemClickedListener(onFabItemClickedListener: OnFabItemClickedListener?) {
        this.onFabItemClickedListener = onFabItemClickedListener
    }

    fun setOverlay(overlay: View?) {
        this.overlay = overlay
        this.overlay!!.setOnClickListener { v: View? -> performToggle() }
    }

    fun setOverlay(colorResurce: Int) {
        val overlayView = View(recyclerView.context.applicationContext)
        overlayView.setBackgroundResource(colorResurce)
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        overlayView.layoutParams = params
        overlayView.visibility = View.GONE
        overlayView.setOnClickListener { v: View? -> performToggle() }
        val parent = floatingActionsMenu!!.parent as ViewGroup
        parent.addView(overlayView, parent.indexOfChild(floatingActionsMenu))
        overlay = overlayView
    }

    companion object {
        const val FAB_ANIMATION_TIME = 250L
    }
}