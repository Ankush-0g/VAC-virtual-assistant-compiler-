package com.example.codecompiler;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

public class ErrorSpan implements LineBackgroundSpan {
    @Override
    public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lineNumber) {
        int oldColor = paint.getColor();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3f);
        
        // Draw wavy or solid underline
        float y = bottom - 2;
        canvas.drawLine(left, y, right, y, paint);
        
        paint.setColor(oldColor);
    }
}
