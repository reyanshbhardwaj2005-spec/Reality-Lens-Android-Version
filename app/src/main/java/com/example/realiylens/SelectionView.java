package com.example.realiylens;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class SelectionView extends View {
    private Paint paint;
    private Paint eraser;
    private RectF selectionRect;
    private float startX, startY;
    private OnSelectionListener listener;
    private boolean isDrawing = false;

    public interface OnSelectionListener {
        void onSelectionFinished(RectF rect);
    }

    public SelectionView(Context context) {
        super(context);
        init();
    }

    public SelectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);

        eraser = new Paint();
        eraser.setAntiAlias(true);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        selectionRect = new RectF();
        
        // Enable hardware acceleration layer for PorterDuff.Mode.CLEAR to work
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw dimmed background
        canvas.drawColor(Color.argb(150, 0, 0, 0));

        if (isDrawing) {
            // Clear the selection area
            canvas.drawRect(selectionRect, eraser);
            // Draw border
            canvas.drawRect(selectionRect, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                selectionRect.set(startX, startY, startX, startY);
                isDrawing = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                selectionRect.set(Math.min(startX, x),
                        Math.min(startY, y),
                        Math.max(startX, x),
                        Math.max(startY, y));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                isDrawing = false;
                if (listener != null) {
                    listener.onSelectionFinished(new RectF(selectionRect));
                }
                selectionRect.setEmpty();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
