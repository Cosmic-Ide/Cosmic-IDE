package com.pranav.completion;

import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.content.Context;
import android.content.res.Resources;

public class KindDrawable extends Drawable {
    
    private final Paint mPaint;
    private final Paint mTextPaint;
    
    private static Resources mResources;
    private final Kind mKind;
    
    public static void setResources(Resources res) {
        mResources = res;
    }
    
    public KindDrawable(Kind kind) {
        mKind = kind;
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(kind.getColor());
        
        mTextPaint = new Paint();
        mTextPaint.setColor(0xffffffff);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(dp(14));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    @Override
    public void draw(Canvas canvas) {
        float width = getBounds().right;
        float height = getBounds().bottom;

        canvas.drawRect(0, 0, width, height, mPaint);

        canvas.save();
        canvas.translate(width / 2f, height / 2f);
        float textCenter = (-(mTextPaint.descent() + mTextPaint.ascent()) / 2f);
        canvas.drawText(mKind.getValue(), 0, textCenter, mTextPaint);
        canvas.restore();
        
    }

    @Override
    public void setAlpha(int p1) {}

    @Override
    public void setColorFilter(ColorFilter p1) {}

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    private float dp(int px) {
        return Math.round(mResources.getDisplayMetrics().density * px);
    }
}