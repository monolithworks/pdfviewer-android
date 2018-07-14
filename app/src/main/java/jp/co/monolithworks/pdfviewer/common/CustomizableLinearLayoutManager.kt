package jp.co.monolithworks.pdfviewer.common

import android.content.Context
import android.util.DisplayMetrics
import android.graphics.PointF
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.support.v7.widget.RecyclerView.*
import android.util.Log



/**
 * Created by adeliae on 7/14/18.
 */
class CustomizableLinearLayoutManager : LinearLayoutManager {

    var scaleFactror = 1.0f

    constructor(context: Context) : super(context) {}
    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: State?): Int {
        return super.scrollVerticallyBy((if (dy > 0) { Math.floor(dy.toDouble() / scaleFactror.toDouble()) } else { Math.ceil(dy.toDouble() / scaleFactror.toDouble()) }).toInt(), recycler, state)
    }

}