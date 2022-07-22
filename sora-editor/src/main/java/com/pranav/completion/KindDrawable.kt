package com.pranav.completion

import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.content.Context
import android.content.res.Resources

class KindDrawable(kind: Kind) : Drawable() {
    
    private val mPaint: Paint
    private val mTextPaint: Paint
    
    private var mResources: Resources?
    private val mKind: Kind

    companion object {
        @JvmStatic
        fun setResources(res: Resources) {
            mResources = res
        }
    }
    
    init {
        mKind = kind
        
        mPaint = Paint()
        mPaint.setAntiAlias(true)
        mPaint.setColor(kind.getColor())
        
        mTextPaint = Paint()
        mTextPaint.setColor(0xffffffff)
        mTextPaint.setAntiAlias(true)
        mTextPaint.setTextSize(dp(14))
        mTextPaint.setTextAlign(Paint.Align.CENTER)
    }
    
    override fun draw(canvas: Canvas) {
        var width: Float = getBounds().right
        var height: Float = getBounds().bottom

        canvas.drawRect(0, 0, width, height, mPaint)

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        var textCenter = (-(mTextPaint.descent() + mTextPaint.ascent()) / 2f)
        canvas.drawText(mKind.getValue(), 0, textCenter, mTextPaint)
        canvas.restore()
        
    }

    override fun setAlpha(p0: Int) {}

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    private fun dp(px: Int): Float {
        return Math.round(mResources?.getDisplayMetrics().density * px)
    }
}