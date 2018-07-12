package jp.co.monolithworks.pdfviewer.common

import android.content.Context
import android.graphics.Canvas
import android.support.v4.app.Fragment
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.util.Log
import jp.co.monolithworks.pdfviewer.ViewerFragment

/**
 * Created by adeliae on 2018/07/11.
 */
class ZoomableRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): RecyclerView(context, attrs, defStyleAttr) {

    private var mActivePointerId = INVALID_POINTER_ID
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1f
    private var maxWidth = 0f
    private var maxHeight = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var width = 0f
    private var height = 0f

    private var _scrollOffsetY = 0

    init {
        mScaleDetector = ScaleGestureDetector(getContext(), ScaleListener())

        addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                _scrollOffsetY += dy
            }
        })
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val velocity = Math.round(velocityY.toDouble() * 0.7).toInt()
        return super.fling(velocityX, velocity)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                mLastTouchX = x
                mLastTouchY = y

                mActivePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {

            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastTouchX = event.getX(newPointerIndex)
                    mLastTouchY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        width = View.MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = View.MeasureSpec.getSize(heightMeasureSpec).toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        val action = event.actionMasked
        mScaleDetector!!.onTouchEvent(event)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                mLastTouchX = x
                mLastTouchY = y

                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                try {
                    val pointerIndex = event.findPointerIndex(mActivePointerId)
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY

                    mPosX += dx

                    if (_scrollOffsetY == 0 || _scrollOffsetY == (computeVerticalScrollRange() - height.toInt())) {
                        mPosY += dy
                    }

                    if (mPosX > 0.0f)
                        mPosX = 0.0f
                    else if (mPosX < maxWidth)
                        mPosX = maxWidth

                    if (mPosY > 0.0f)
                        mPosY = 0.0f
                    else if (mPosY < (maxHeight))
                        mPosY = maxHeight

                    invalidate()

                    mLastTouchX = x
                    mLastTouchY = y


                } catch (e: Exception) {

                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastTouchX = event.getX(newPointerIndex)
                    mLastTouchY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }

        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        if (mScaleFactor == 1.0f) {
            mPosX = 0.0f
            mPosY = 0.0f
        }

        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor)
        super.dispatchDraw(canvas)
        canvas.restore()

        invalidate()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScaleFactor = mScaleFactor
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 5.0f))
            maxWidth = width - width * mScaleFactor
            maxHeight = height - height * mScaleFactor

            val adjustedScaleFactor = mScaleFactor / prevScaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY
            mPosX += (mPosX - focusX) * (adjustedScaleFactor - 1)
            mPosY += (mPosY - focusY) * (adjustedScaleFactor - 1)

            (adapter as? ViewerFragment.PageAdapter)?.let {
                it.scaleFactor = mScaleFactor
            }

            invalidate()
            return true
        }
    }

    companion object {
        private val INVALID_POINTER_ID = -1
    }

}
