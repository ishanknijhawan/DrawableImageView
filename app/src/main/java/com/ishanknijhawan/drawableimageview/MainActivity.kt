package com.ishanknijhawan.drawableimageview

import android.graphics.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream


class MainActivity : AppCompatActivity(), ColorPickerDialog.OnColorChangedListener {

    private lateinit var drawableImageView: DrawableImageView
    lateinit var mPaint: Paint
    private var mEmboss: MaskFilter? = null
    private var mBlur: MaskFilter? = null
    lateinit var outputStream: FileOutputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawableImageView = findViewById(R.id.iv_draw)
        mPaint = drawableImageView.mPaint

        mEmboss = EmbossMaskFilter(
            floatArrayOf(1f, 1f, 1f),
            0.4f, 6F, 3.5f
        )
        mBlur = BlurMaskFilter(8F, BlurMaskFilter.Blur.NORMAL)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, FingerPaintActivity.COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c')
        menu.add(0, FingerPaintActivity.EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's')
        menu.add(0, FingerPaintActivity.BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z')
        menu.add(0, FingerPaintActivity.ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z')
        menu.add(0, FingerPaintActivity.SRCATOP_MENU_ID, 0, "SrcATop").setShortcut('5', 'z')
        menu.add(0, FingerPaintActivity.Save, 0, "Save").setShortcut('5', 'z')
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mPaint.xfermode = null
        mPaint.alpha = 0xFF
        //to get bitmap from drawingView, use drawingView.drawingCache
        when (item.itemId) {
            FingerPaintActivity.COLOR_MENU_ID -> {
                ColorPickerDialog(this, this, mPaint.color).show()
                return true
            }
            FingerPaintActivity.EMBOSS_MENU_ID -> {
                if (mPaint.maskFilter !== mEmboss) {
                    mPaint.maskFilter = mEmboss
                } else {
                    mPaint.maskFilter = null
                }
                return true
            }
            FingerPaintActivity.BLUR_MENU_ID -> {
                if (mPaint.maskFilter !== mBlur) {
                    mPaint.maskFilter = mBlur
                } else {
                    mPaint.maskFilter = null
                }
                return true
            }
            FingerPaintActivity.ERASE_MENU_ID -> {
                mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                mPaint.alpha = 0x80
                return true
            }
            FingerPaintActivity.SRCATOP_MENU_ID -> {
                mPaint.xfermode = PorterDuffXfermode(
                    PorterDuff.Mode.SRC_ATOP
                )
                mPaint.alpha = 0x80
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun colorChanged(color: Int) {
        mPaint.color = color
    }
}