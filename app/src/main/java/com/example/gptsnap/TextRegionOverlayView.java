package com.example.gptsnap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextRegionOverlayView extends View {
    private List<RectF> textRegions;
    private RectF selectedRegion;
    private Paint boxPaint;

    public TextRegionOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);

        textRegions = new ArrayList<>();
        selectedRegion = null;
    }

    public void setTextRegions(List<RectF> regions) {
        textRegions = regions;
        invalidate();
    }

    @Nullable
    public RectF getSelectedRegion() {
        return selectedRegion;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RectF region : textRegions) {
            canvas.drawRect(region, boxPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            selectedRegion = null;
            for (RectF region : textRegions) {
                if (region.contains(x, y)) {
                    selectedRegion = region;
                    break;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
