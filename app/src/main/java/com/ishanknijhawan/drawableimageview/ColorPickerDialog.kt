package com.ishanknijhawan.drawableimageview

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.roundToInt


open class ColorPickerDialog(
    context: Context?,
    private val mListener: OnColorChangedListener,
    private val mInitialColor: Int
) : Dialog(context!!) {
    interface OnColorChangedListener {
        fun colorChanged(color: Int)
    }

    private open class ColorPickerView(
        c: Context?,
        private val mListener: OnColorChangedListener,
        color: Int
    ) :
        View(c) {
        private val mPaint: Paint
        private val mCenterPaint: Paint
        private val mColors: IntArray = intArrayOf(
            -0x10000, -0xff01, -0xffff01, -0xff0001, -0xff0100,
            -0x100, -0x10000
        )
        private var mTrackingCenter = false
        private var mHighlightCenter = false

        override fun onDraw(canvas: Canvas) {
            val r: Float = CENTER_X - mPaint.strokeWidth * 0.5f
            canvas.translate(CENTER_X.toFloat(), CENTER_X.toFloat())
            canvas.drawOval(RectF(-r, -r, r, r), mPaint)
            canvas.drawCircle(0F, 0F, CENTER_RADIUS.toFloat(), mCenterPaint)
            if (mTrackingCenter) {
                val c: Int = mCenterPaint.color
                mCenterPaint.style = Paint.Style.STROKE
                if (mHighlightCenter) {
                    mCenterPaint.alpha = 0xFF
                } else {
                    mCenterPaint.alpha = 0x80
                }
                canvas.drawCircle(
                    0F, 0F,
                    CENTER_RADIUS + mCenterPaint.strokeWidth,
                    mCenterPaint
                )
                mCenterPaint.style = Paint.Style.FILL
                mCenterPaint.color = c
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(CENTER_X * 2, CENTER_Y * 2)
        }

        private fun floatToByte(x: Float): Int {
            return x.roundToInt()
        }

        private fun pinToByte(n: Int): Int {
            var n = n
            if (n < 0) {
                n = 0
            } else if (n > 255) {
                n = 255
            }
            return n
        }

        private fun ave(s: Int, d: Int, p: Float): Int {
            return s + Math.round(p * (d - s))
        }

        private fun interpColor(colors: IntArray, unit: Float): Int {
            if (unit <= 0) {
                return colors[0]
            }
            if (unit >= 1) {
                return colors[colors.size - 1]
            }
            var p = unit * (colors.size - 1)
            val i = p.toInt()
            p -= i.toFloat()

            // now p is just the fractional part [0...1) and i is the index
            val c0 = colors[i]
            val c1 = colors[i + 1]
            val a = ave(Color.alpha(c0), Color.alpha(c1), p)
            val r = ave(Color.red(c0), Color.red(c1), p)
            val g = ave(Color.green(c0), Color.green(c1), p)
            val b = ave(Color.blue(c0), Color.blue(c1), p)
            return Color.argb(a, r, g, b)
        }

        private fun rotateColor(color: Int, rad: Float): Int {
            val deg = rad * 180 / 3.1415927f
            val r: Int = Color.red(color)
            val g: Int = Color.green(color)
            val b: Int = Color.blue(color)
            val cm = ColorMatrix()
            val tmp = ColorMatrix()
            cm.setRGB2YUV()
            tmp.setRotate(0, deg)
            cm.postConcat(tmp)
            tmp.setYUV2RGB()
            cm.postConcat(tmp)
            val a = cm.array
            val ir = floatToByte(a[0] * r + a[1] * g + a[2] * b)
            val ig = floatToByte(a[5] * r + a[6] * g + a[7] * b)
            val ib = floatToByte(a[10] * r + a[11] * g + a[12] * b)
            return Color.argb(
                Color.alpha(color), pinToByte(ir),
                pinToByte(ig), pinToByte(ib)
            )
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x - CENTER_X
            val y = event.y - CENTER_Y
            val inCenter = Math.sqrt((x * x + y * y).toDouble()) <= CENTER_RADIUS
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mTrackingCenter = inCenter
                    if (inCenter) {
                        mHighlightCenter = true
                        invalidate()
                    }
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter
                            invalidate()
                        }
                    } else {
                        val angle =
                            Math.atan2(y.toDouble(), x.toDouble()).toFloat()
                        // need to turn angle [-PI ... PI] into unit [0....1]
                        var unit = angle / (2 * PI)
                        if (unit < 0) {
                            unit += 1f
                        }
                        mCenterPaint.color = interpColor(mColors, unit)
                        invalidate()
                    }
                }
                MotionEvent.ACTION_MOVE -> if (mTrackingCenter) {
                    if (mHighlightCenter != inCenter) {
                        mHighlightCenter = inCenter
                        invalidate()
                    }
                } else {
                    val angle = atan2(y.toDouble(), x.toDouble()).toFloat()
                    var unit = angle / (2 * PI)
                    if (unit < 0) {
                        unit += 1f
                    }
                    mCenterPaint.color = interpColor(mColors, unit)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> if (mTrackingCenter) {
                    if (inCenter) {
                        mListener.colorChanged(mCenterPaint.color)
                    }
                    mTrackingCenter = false // so we draw w/o halo
                    invalidate()
                }
            }
            return true
        }

        companion object {
            private const val CENTER_X = 100
            private const val CENTER_Y = 100
            private const val CENTER_RADIUS = 32
            private const val PI = 3.1415926f
        }

        init {
            val s: Shader = SweepGradient(0F, 0F, mColors, null)
            mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mPaint.shader = s
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = 32F
            mCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mCenterPaint.color = color
            mCenterPaint.strokeWidth = 5F
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val l: OnColorChangedListener = object : OnColorChangedListener {
            override fun colorChanged(color: Int) {
                mListener.colorChanged(color)
                dismiss()
            }
        }
        setContentView(ColorPickerView(context, l, mInitialColor))
        setTitle("Pick a Color")
    }
}
