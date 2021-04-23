package com.ishanknijhawan.drawableimageview

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import com.ishanknijhawan.drawableimageview.ColorPickerDialog.OnColorChangedListener
import java.io.File
import java.io.FileOutputStream


class FingerPaintActivity : Activity(), OnColorChangedListener {
    var mv: MyView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mv = MyView(this)
        mv!!.isDrawingCacheEnabled = true
        mv!!.setBackgroundResource(R.drawable.pexels) //set the back ground if you wish to
        setContentView(mv)
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.color = -0x10000
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        mPaint!!.strokeWidth = 20F
        actionBar?.show()
        mEmboss = EmbossMaskFilter(
            floatArrayOf(1f, 1f, 1f),
            0.4f, 6F, 3.5f
        )
        mBlur = BlurMaskFilter(8F, BlurMaskFilter.Blur.NORMAL)
    }

    private var mPaint: Paint? = null
    private var mEmboss: MaskFilter? = null
    private var mBlur: MaskFilter? = null
    override fun colorChanged(color: Int) {
        mPaint!!.color = color
    }

    inner class MyView(ccontext: Context) : View(
        ccontext
    ) {
        private var mBitmap: Bitmap? = null
        private var mCanvas: Canvas? = null
        private val mPath: Path = Path()
        private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBitmap!!)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawBitmap(mBitmap!!, 0F, 0F, mBitmapPaint)
            canvas.drawPath(mPath, mPaint!!)
        }

        private var mX = 0f
        private var mY = 0f
        private fun touch_start(x: Float, y: Float) {
            //showDialog();
            mPath.reset()
            mPath.moveTo(x, y)
            mX = x
            mY = y
        }

        private fun touch_move(x: Float, y: Float) {
            val dx = Math.abs(x - mX)
            val dy = Math.abs(y - mY)
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mX = x
                mY = y
            }
        }

        private fun touch_up() {
            mPath.lineTo(mX, mY)
            // commit the path to our offscreen
            mCanvas!!.drawPath(mPath, mPaint!!)
            // kill this so we don't double draw
            mPath.reset()
            mPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            //mPaint.setMaskFilter(null);
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touch_start(x, y)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    touch_move(x, y)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    touch_up()
                    invalidate()
                }
            }
            return true
        }

        private val TOUCH_TOLERANCE = 4f

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c')
        menu.add(0, EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's')
        menu.add(0, BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z')
        menu.add(0, ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z')
        menu.add(0, SRCATOP_MENU_ID, 0, "SrcATop").setShortcut('5', 'z')
        menu.add(0, Save, 0, "Save").setShortcut('5', 'z')
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mPaint!!.xfermode = null
        mPaint!!.alpha = 0xFF
        when (item.itemId) {
            COLOR_MENU_ID -> {
                ColorPickerDialog(this, this, mPaint!!.color).show()
                return true
            }
            EMBOSS_MENU_ID -> {
                if (mPaint!!.maskFilter !== mEmboss) {
                    mPaint!!.maskFilter = mEmboss
                } else {
                    mPaint!!.maskFilter = null
                }
                return true
            }
            BLUR_MENU_ID -> {
                if (mPaint!!.maskFilter !== mBlur) {
                    mPaint!!.maskFilter = mBlur
                } else {
                    mPaint!!.maskFilter = null
                }
                return true
            }
            ERASE_MENU_ID -> {
                mPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                mPaint!!.alpha = 0x80
                return true
            }
            SRCATOP_MENU_ID -> {
                mPaint!!.xfermode = PorterDuffXfermode(
                    PorterDuff.Mode.SRC_ATOP
                )
                mPaint!!.alpha = 0x80
                return true
            }
            Save -> {
                val editalert = AlertDialog.Builder(this@FingerPaintActivity)
                editalert.setTitle("Please Enter the name with which you want to Save")
                val input = EditText(this@FingerPaintActivity)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT
                )
                input.layoutParams = lp
                editalert.setView(input)
                editalert.setPositiveButton(
                    "OK"
                ) { dialog, whichButton ->
                    val name = input.text.toString()
                    val bitmap = mv!!.drawingCache
                    val path = Environment.getExternalStorageDirectory().absolutePath
                    val file = File("/sdcard/$name.png")
                    try {
                        if (!file.exists()) {
                            file.createNewFile()
                        }
                        val ostream = FileOutputStream(file)
                        bitmap.compress(CompressFormat.PNG, 10, ostream)
                        ostream.close()
                        mv!!.invalidate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        mv!!.isDrawingCacheEnabled = false
                    }
                }
                editalert.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val COLOR_MENU_ID = Menu.FIRST
        const val EMBOSS_MENU_ID = Menu.FIRST + 1
        const val BLUR_MENU_ID = Menu.FIRST + 2
        const val ERASE_MENU_ID = Menu.FIRST + 3
        const val SRCATOP_MENU_ID = Menu.FIRST + 4
        const val Save = Menu.FIRST + 5
    }
}