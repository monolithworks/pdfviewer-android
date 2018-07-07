package jp.co.monolithworks.imagegenerator

import android.content.Context
import android.graphics.Canvas
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View


/**
 * Created by adeliae on 4/10/18.
 */

class ZoomableRecyclerView : RecyclerView {

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

    constructor(context: Context) : super(context) {
        mScaleDetector = ScaleGestureDetector(getContext(), ScaleListener())
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mScaleDetector = ScaleGestureDetector(getContext(), ScaleListener())
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        mScaleDetector = ScaleGestureDetector(getContext(), ScaleListener())
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(event)
        mScaleDetector!!.onTouchEvent(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                mLastTouchX = x
                mLastTouchY = y

                mActivePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId < 0 || !(event.pointerCount > mActivePointerId)) {
                    mActivePointerId = event.getPointerId(0)
                }

                val pointerIndex = event.findPointerIndex(mActivePointerId)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY

                mPosX += dx
                mPosY += dy

                if (mPosX > 0.0f)
                    mPosX = 0.0f
                else if (mPosX < maxWidth)
                    mPosX = maxWidth

                if (mPosY > 0.0f)
                    mPosY = 0.0f
                else if (mPosY < maxHeight)
                    mPosY = maxHeight

                mLastTouchX = x
                mLastTouchY = y

                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_CANCEL -> {
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
        val action = MotionEventCompat.getActionMasked(event)
        mScaleDetector!!.onTouchEvent(event)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                mLastTouchX = x
                mLastTouchY = y

                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId < 0 || !(event.pointerCount > mActivePointerId)) {
                    mActivePointerId = event.getPointerId(0)
                }

                val pointerIndex = event.findPointerIndex(mActivePointerId)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY

                mPosX += dx
                mPosY += dy

                if (mPosX > 0.0f)
                    mPosX = 0.0f
                else if (mPosX < maxWidth)
                    mPosX = maxWidth

                if (mPosY > 0.0f)
                    mPosY = 0.0f
                else if (mPosY < maxHeight)
                    mPosY = maxHeight

                mLastTouchX = x
                mLastTouchY = y

                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_CANCEL -> {
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
        canvas.save(Canvas.ALL_SAVE_FLAG)
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
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 3.0f))
            maxWidth = width - width * mScaleFactor
            maxHeight = height - height * mScaleFactor

            val adjustedScaleFactor = mScaleFactor / prevScaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY
            mPosX += (mPosX - focusX) * (adjustedScaleFactor - 1)
            mPosY += (mPosY - focusY) * (adjustedScaleFactor - 1)

            invalidate()
            return true
        }
    }

    companion object {
        private val INVALID_POINTER_ID = -1
    }

}
